"""Token de serviço exigido do backend-core (ETA-010)."""
import hmac

from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

bearer = HTTPBearer(auto_error=False)


def require_service_token(request: Request,
                          credentials: HTTPAuthorizationCredentials | None = Depends(bearer)) -> None:
    expected = request.app.state.settings.service_token.encode()
    if credentials is None or not hmac.compare_digest(credentials.credentials.encode(), expected):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token de serviço inválido.",
                            headers={"WWW-Authenticate": "Bearer"})
