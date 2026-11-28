#!/usr/bin/env python3
"""Verifica a rastreabilidade entre os requisitos das specs e os testes (constituição, artigo 1).

- Cada spec em specs/NNN-nome/spec.md tem cabeçalho válido (id, title, prefix, status).
- IDs de requisito (PREFIXO-NNN) usam o prefixo da spec e são únicos no repositório.
- Todo marcador `@req ID` nos testes aponta para um requisito que existe e não foi removido.
- Spec `implemented`: todo requisito que não está [pendente] nem [removido] tem pelo menos um teste.
- Spec `approved`: requisitos ainda sem teste são só informados.

Uso: python3 scripts/check_spec_traceability.py [--report]
Sem dependências além da biblioteca padrão.
"""
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPECS_DIR = ROOT / "specs"
TEST_ROOTS = [
    ROOT / "backend-core" / "src" / "test",
    ROOT / "ai-engine" / "tests",
    ROOT / "smartroute-frontend" / "src",
    ROOT / "smartroute-frontend" / "e2e",
]
TEST_SUFFIXES = {".java", ".py", ".ts", ".tsx"}
IGNORED_DIRS = {"node_modules", "__pycache__", ".venv", "target", "dist"}

STATUSES = {"draft", "approved", "implemented", "superseded"}
REQ_ID = r"[A-Z][A-Z0-9]*-\d{3}"
REQUIREMENT_LINE = re.compile(rf"^\s*-\s+\*\*({REQ_ID})\*\*\s*(?:\[(pendente|removido)\])?")
REQ_MARKER = re.compile(rf"@req\s+({REQ_ID}(?:\s*,\s*{REQ_ID})*)")
SPEC_DIR_NAME = re.compile(r"^(\d{3})-[a-z0-9-]+$")


@dataclass
class Requirement:
    id: str
    spec: str
    flag: str | None  # None, "pendente" ou "removido"
    tests: list[str] = field(default_factory=list)


@dataclass
class Spec:
    path: Path
    meta: dict[str, str]
    requirements: list[Requirement]

    @property
    def name(self) -> str:
        return self.path.parent.name


def parse_front_matter(text: str) -> dict[str, str] | None:
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return None
    meta: dict[str, str] = {}
    for line in lines[1:]:
        if line.strip() == "---":
            return meta
        content = line.split("#", 1)[0].strip()
        if ":" in content:
            key, value = content.split(":", 1)
            meta[key.strip()] = value.strip().strip('"').strip("'")
    return None


def load_specs(errors: list[str]) -> list[Spec]:
    specs = []
    for path in sorted(SPECS_DIR.glob("*/spec.md")):
        if path.parent.name.startswith("_"):
            continue
        rel = path.relative_to(ROOT)
        text = path.read_text(encoding="utf-8")
        meta = parse_front_matter(text)
        if meta is None:
            errors.append(f"{rel}: cabeçalho (front matter entre ---) ausente ou não fechado")
            continue
        for key in ("id", "title", "prefix", "status"):
            if not meta.get(key):
                errors.append(f"{rel}: campo '{key}' ausente no cabeçalho")
        match = SPEC_DIR_NAME.match(path.parent.name)
        if not match:
            errors.append(f"{rel}: a pasta deve se chamar NNN-nome-curto (minúsculas e hífens)")
        elif meta.get("id") and meta["id"] != match.group(1):
            errors.append(f"{rel}: id '{meta['id']}' difere do número da pasta '{match.group(1)}'")
        if meta.get("status") and meta["status"] not in STATUSES:
            errors.append(f"{rel}: status '{meta['status']}' inválido (use {', '.join(sorted(STATUSES))})")

        requirements = []
        for number, line in enumerate(text.splitlines(), start=1):
            found = REQUIREMENT_LINE.match(line)
            if not found:
                continue
            req_id, flag = found.groups()
            if meta.get("prefix") and not req_id.startswith(meta["prefix"] + "-"):
                errors.append(f"{rel}:{number}: {req_id} não usa o prefixo da spec ({meta['prefix']})")
            requirements.append(Requirement(req_id, path.parent.name, flag))
        specs.append(Spec(path, meta, requirements))
    return specs


def iter_test_files():
    for root in TEST_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if path.suffix not in TEST_SUFFIXES or not path.is_file():
                continue
            if IGNORED_DIRS.intersection(path.relative_to(ROOT).parts):
                continue
            yield path


def collect_markers() -> dict[str, list[str]]:
    markers: dict[str, list[str]] = {}
    for path in iter_test_files():
        rel = path.relative_to(ROOT)
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            for group in REQ_MARKER.findall(line):
                for req_id in re.findall(REQ_ID, group):
                    markers.setdefault(req_id, []).append(f"{rel}:{number}")
    return markers


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--report", action="store_true", help="mostra cada requisito e os testes que o cobrem")
    args = parser.parse_args()

    errors: list[str] = []
    warnings: list[str] = []
    specs = load_specs(errors)

    prefixes: dict[str, str] = {}
    for spec in specs:
        prefix = spec.meta.get("prefix")
        if prefix and prefix in prefixes:
            errors.append(f"prefixo {prefix} usado em {prefixes[prefix]} e {spec.name}")
        elif prefix:
            prefixes[prefix] = spec.name

    requirements: dict[str, Requirement] = {}
    for spec in specs:
        for req in spec.requirements:
            if req.id in requirements:
                errors.append(f"{req.id} declarado mais de uma vez ({requirements[req.id].spec} e {spec.name})")
            requirements[req.id] = req

    for req_id, places in collect_markers().items():
        req = requirements.get(req_id)
        if req is None:
            errors.append(f"@req {req_id} em {', '.join(places)} não existe em nenhuma spec")
        elif req.flag == "removido":
            errors.append(f"@req {req_id} em {', '.join(places)} aponta para requisito removido")
        else:
            req.tests.extend(places)

    for spec in specs:
        status = spec.meta.get("status")
        uncovered = [r.id for r in spec.requirements if r.flag is None and not r.tests]
        if not spec.requirements and status in {"approved", "implemented"}:
            errors.append(f"{spec.name}: spec {status} sem nenhum requisito")
        if not uncovered:
            continue
        if status == "implemented":
            errors.append(f"{spec.name} está implemented, mas sem teste com @req: {', '.join(uncovered)}")
        elif status == "approved":
            warnings.append(f"{spec.name}: ainda sem teste: {', '.join(uncovered)}")

    if args.report:
        for spec in specs:
            print(f"\n{spec.name} [{spec.meta.get('status')}] — {spec.meta.get('title')}")
            for req in spec.requirements:
                label = f" [{req.flag}]" if req.flag else ""
                print(f"  {req.id}{label}: {len(req.tests)} teste(s)")
                for place in req.tests:
                    print(f"      {place}")

    for warning in warnings:
        print(f"aviso: {warning}")
    for error in errors:
        print(f"erro: {error}", file=sys.stderr)

    total = sum(len(s.requirements) for s in specs)
    covered = sum(1 for r in requirements.values() if r.tests)
    print(f"\n{len(specs)} specs, {total} requisitos, {covered} com teste, {len(errors)} erro(s).")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
