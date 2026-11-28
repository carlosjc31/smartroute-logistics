"""Contrato OpenAPI do ai-engine (ADR 0009): exportado do FastAPI e versionado em contracts/ai-engine.

`python -m app.contract` regrava contracts/ai-engine/openapi.json. `tests/test_contract.py` falha se o código
divergir do arquivo versionado, e o CI roda oasdiff contra a main.
"""
import json
from pathlib import Path

CONTRACT_PATH = Path(__file__).resolve().parents[2] / "contracts" / "ai-engine" / "openapi.json"
HTTP_METHODS = {"get", "post", "put", "patch", "delete"}
CONSTRAINTS = ("minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "enum", "const")


def normalize(spec: dict) -> dict:
    """Só o que o backend-core depende: rotas, campos, tipos, obrigatórios e limites. Ignora textos e a
    forma como cada versão do FastAPI escreve o documento, para o teste não quebrar a cada atualização."""
    schemas = spec.get("components", {}).get("schemas", {})

    def resolve(schema: dict) -> dict:
        while "$ref" in schema:
            schema = schemas[schema["$ref"].rsplit("/", 1)[1]]
        return schema

    def shape(schema: dict):
        schema = resolve(schema)
        if "properties" in schema:
            return {
                "required": sorted(schema.get("required", [])),
                "properties": {name: shape(prop) for name, prop in sorted(schema["properties"].items())},
            }
        return {"type": schema.get("type"), **{key: schema[key] for key in CONSTRAINTS if key in schema}}

    operations = {}
    for path, item in sorted(spec["paths"].items()):
        for method, operation in sorted(item.items()):
            if method not in HTTP_METHODS:
                continue
            body = operation.get("requestBody", {}).get("content", {}).get("application/json", {}).get("schema")
            responses = {}
            for code, response in sorted(operation.get("responses", {}).items()):
                schema = response.get("content", {}).get("application/json", {}).get("schema")
                responses[code] = shape(schema) if schema and code.startswith("2") else None
            operations[f"{method.upper()} {path}"] = {
                "request": shape(body) if body else None,
                "responses": responses,
                "authenticated": bool(operation.get("security")),
            }
    return operations


def export() -> None:
    from app.main import app

    CONTRACT_PATH.write_text(json.dumps(app.openapi(), indent=2, ensure_ascii=False, sort_keys=True) + "\n",
                             encoding="utf-8")
    print(f"Contrato gravado em {CONTRACT_PATH}")


if __name__ == "__main__":
    export()
