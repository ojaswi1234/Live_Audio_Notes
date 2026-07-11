# LLMLingua Compression Server

This is a FastAPI-based server that exposes [Microsoft LLMLingua](https://github.com/microsoft/LLMLingua) via a REST API to compress your prompts and save tokens.

## Deployment on Render

1. Create a new **Web Service** on Render.
2. Connect your GitHub repository containing this code.
3. Set the **Root Directory** to `backend-llmlingua`.
4. Render will automatically detect `requirements.txt` and use Python.
5. **Start Command:** `uvicorn main:app --host 0.0.0.0 --port $PORT`

### Important Warnings
* **Memory Limits:** LLMLingua loads machine learning models (like `microsoft/llmlingua-2-xlm-roberta-large-meetingbank`) into memory using PyTorch. 
* **Render Free Tier (512MB RAM)** is usually **NOT ENOUGH** to run this server and you will likely get an Out Of Memory (OOM) error. You will likely need a paid plan with at least 2GB of RAM.

## API Endpoint Used by EchoReader

**POST** `/api/compress`

**Body:**
```json
{
  "context": ["Your long text chunk goes here..."],
  "instruction": "Compress this text for analysis.",
  "target_token": 300,
  "dynamic_context_compression_ratio": 0.3
}
```

**Response:**
```json
{
  "compressed_prompt": "Your compressed text...",
  "origin_tokens": 500,
  "compressed_tokens": 200,
  "ratio": "2.5x"
}
```

Once deployed, copy the Render URL (e.g., `https://my-llmlingua-server.onrender.com/api/compress`) and paste it into the **EchoReader App Settings** under the LLMLingua Compression URL field.
