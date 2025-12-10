from fastapi import FastAPI, HTTPException, Header, Depends
from pydantic import BaseModel
import jwt
import os
from dotenv import load_dotenv
import uvicorn

# Carrega variáveis do .env
load_dotenv()

app = FastAPI()

#SECRET_KEY = os.getenv("JWT_SECRET")#

SECRET_KEY = "UmaSenhaMuitoSeguraEGrandeParaCriptografiaHMAC256"

# Modelo de dados (Define o JSON que esperamos receber)
class EmailSchema(BaseModel):
    destinatario: str
    assunto: str
    corpo: str

# Função para validar o Token JWT
def verificar_token(authorization: str = Header(None)):
    if not authorization:
        raise HTTPException(status_code=401, detail="Token não fornecido")
    
    try:
        # O header vem como "Bearer eyJhbGci..."
        scheme, token = authorization.split()
        if scheme.lower() != 'bearer':
            raise HTTPException(status_code=401, detail="Formato de token inválido")
            
        # Decodifica e valida usando o Segredo Compartilhado
        # Verifica se o segredo existe para evitar erro silencioso
        if not SECRET_KEY:
            raise HTTPException(status_code=500, detail="Erro interno: JWT_SECRET não configurado")

        payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256", "HS384"])
        return payload  # Retorna os dados do usuário (id, email, role)
        
    except (ValueError, jwt.ExpiredSignatureError, jwt.InvalidTokenError) as e:
        print(f"Erro de validação: {e}")
        raise HTTPException(status_code=403, detail="Token inválido ou expirado")

@app.get("/")
def health_check():
    return {"status": "Email Service (Python) rodando"}

@app.post("/emails")
def enviar_email(email: EmailSchema, usuario: dict = Depends(verificar_token)):
    # Simulação do envio
    print("\n" + "="*40)
    print("📧  PYTHON EMAIL SERVICE")
    print("="*40)
    print(f"DE: Sistema de Eventos")
    print(f"PARA: {email.destinatario}")
    print(f"ASSUNTO: {email.assunto}")
    print("-" * 20)
    print(f"{email.corpo}")
    print("-" * 20)
    print(f"Solicitado por: {usuario.get('sub')} (Role: {usuario.get('role')})")
    print("="*40 + "\n")
    
    return {"message": "E-mail enviado com sucesso", "status": "enviado"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8090)
