from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from llmlingua import PromptCompressor
import os

app = FastAPI(title="LLMLingua Compression API")

# Initialize compressor globally
# Note: Render free tier (512MB RAM) may struggle to load this model.
# Consider using a smaller model or deploying on a paid tier with >= 2GB RAM.
compressor = None

@app.on_event("startup")
def load_model():
    global compressor
    # We use LLMLingua-2 small model by default to save memory
    model_name = os.getenv("MODEL_NAME", "microsoft/llmlingua-2-xlm-roberta-large-meetingbank")
    try:
        print(f"Loading model {model_name}...")
        compressor = PromptCompressor(
            model_name=model_name, 
            use_llmlingua2=True,
            device_map="cpu" # Force CPU for cheap cloud deployments
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
