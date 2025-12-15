from fastapi import FastAPI, HTTPException, Header, Depends
from pydantic import BaseModel
import jwt
import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from dotenv import load_dotenv
import uvicorn

# Carrega variáveis do .env (se existirem localmente)
load_dotenv()

app = FastAPI()

SECRET_KEY = "UmaSenhaMuitoSeguraEGrandeParaCriptografiaHMAC256"

# Configurações de SMTP (Pegando das variáveis de ambiente)
SMTP_HOST = os.getenv("SMTP_HOST", "smtp.gmail.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", 587))
SMTP_USER = os.getenv("SMTP_USER") # Seu email
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD") # Sua senha de App

class EmailSchema(BaseModel):
    destinatario: str
    assunto: str
    corpo: str

def verificar_token(authorization: str = Header(None)):
    if not authorization:
        raise HTTPException(status_code=401, detail="Token não fornecido")
    
    try:
        scheme, token = authorization.split()
        if scheme.lower() != 'bearer':
            raise HTTPException(status_code=401, detail="Formato de token inválido")
            
        payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256", "HS384"])
        return payload
        
    except (ValueError, jwt.ExpiredSignatureError, jwt.InvalidTokenError) as e:
        print(f"Erro de validação: {e}")
        raise HTTPException(status_code=403, detail="Token inválido ou expirado")

def enviar_email_real(destinatario, assunto, corpo):
    """Função interna para disparar o e-mail via SMTP"""
    if not SMTP_USER or not SMTP_PASSWORD:
        print("!!! ERRO: Credenciais de e-mail não configuradas no Docker.")
        return False

    try:
        msg = MIMEMultipart()
        msg['From'] = SMTP_USER
        msg['To'] = destinatario
        msg['Subject'] = assunto

        msg.attach(MIMEText(corpo, 'plain'))

        # Conexão com o servidor SMTP do Gmail
        server = smtplib.SMTP(SMTP_HOST, SMTP_PORT)
        server.starttls() # Criptografia TLS
        server.login(SMTP_USER, SMTP_PASSWORD)
        text = msg.as_string()
        server.sendmail(SMTP_USER, destinatario, text)
        server.quit()
        return True
    except Exception as e:
        print(f"!!! Erro ao conectar no SMTP: {e}")
        return False

@app.get("/")
def health_check():
    return {"status": "Email Service (Python) rodando com SMTP"}

@app.post("/emails")
def enviar_email(email: EmailSchema, usuario: dict = Depends(verificar_token)):
    print(f"\nTentando enviar email para: {email.destinatario}")
    
    sucesso = enviar_email_real(email.destinatario, email.assunto, email.corpo)
    
    if sucesso:
        print(">>> E-mail enviado com sucesso via SMTP!")
        return {"message": "E-mail enviado com sucesso", "status": "enviado"}
    else:
        # Não travamos a requisição com erro 500, mas avisamos que falhou o envio real
        print("!!! Falha no envio via SMTP (verifique logs)")
        return {"message": "Erro ao enviar e-mail real (verifique logs)", "status": "falha_smtp"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8090)