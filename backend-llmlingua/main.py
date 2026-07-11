from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from llmlingua import PromptCompressor
import os
import torch

# Limit PyTorch threads to save memory overhead on small containers
torch.set_num_threads(1)

app = FastAPI(title="LLMLingua Compression API")

# Initialize compressor globally
compressor = None

@app.on_event("startup")
def load_model():
    global compressor
    # We use distilgpt2 which is much smaller (~300MB) instead of the large roberta model
    # to help it fit within Render's 512MB free tier memory limit.
    model_name = os.getenv("MODEL_NAME", "distilgpt2")
    try:
        print(f"Loading model {model_name}...")
        compressor = PromptCompressor(
            model_name=model_name, 
            use_llmlingua2=False, # False because distilgpt2 uses the original LLMLingua approach
            device_map="cpu" 
        )
        print("Model loaded successfully!")
    except Exception as e:
        print(f"Error loading model: {e}")

class CompressionRequest(BaseModel):
    context: list[str]
    instruction: str = ""
    question: str = ""
    target_token: int = 300
    dynamic_context_compression_ratio: float = 0.3

@app.post("/api/compress")
async def compress_prompt(req: CompressionRequest):
    if compressor is None:
        raise HTTPException(status_code=500, detail="Compressor not initialized or out of memory")
    
    try:
        results = compressor.compress_prompt(
            context=req.context,
            instruction=req.instruction,
            question=req.question,
            target_token=req.target_token,
            rank_method="longllmlingua",
            dynamic_context_compression_ratio=req.dynamic_context_compression_ratio
        )
        return results
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
def health_check():
    return {
        "status": "active",
        "model_loaded": compressor is not None
    }

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
