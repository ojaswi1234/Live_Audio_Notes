const express = require('express');
const cors = require('cors');
const { OpenAI } = require('openai');
const NodeCache = require('node-cache');
const multer = require('multer');
const fs = require('fs');
const crypto = require('crypto');
require('dotenv').config();

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// In-memory cache for token saving (TTL: 2 hours)
const cache = new NodeCache({ stdTTL: 7200 });

// Setup multer for voice inputs (audio files)
const upload = multer({ dest: 'uploads/' });

// Base URL for LiteLLM or upstream provider
const liteLLMBaseUrl = process.env.LITELLM_BASE_URL || 'https://api.groq.com/openai/v1';

// Middleware to extract dynamic API key from the request header
const requireDynamicApiKey = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Dynamic API key is required in the Authorization header (Bearer <key>)' });
  }
  req.apiKey = authHeader.split(' ')[1];
  next();
};

// Helper function to initialize OpenAI client per request
const getOpenAIClient = (apiKey) => {
  return new OpenAI({
    apiKey: apiKey,
    baseURL: liteLLMBaseUrl,
  });
};

// --- API Endpoints ---

app.get('/api/status', (req, res) => {
  res.json({
    proxyStatus: 'active',
    upstreamBaseUrl: liteLLMBaseUrl,
    message: 'Send requests with Authorization: Bearer <YOUR_DYNAMIC_API_KEY>'
  });
});

// 1. Structured Notes / Analysis endpoint with Caching
app.post('/api/analyze', requireDynamicApiKey, async (req, res) => {
  const { chunkText, bookTitle, bookAuthor, readingPurpose, depthLevel, focusArea, previousMasterNotes, modelId } = req.body;
  
  // Cache key based on input text and focus to save tokens if re-requested
  const cacheKey = crypto.createHash('md5').update(
    JSON.stringify({ chunkText, focusArea, depthLevel, previousMasterNotes })
  ).digest('hex');

  const cachedResult = cache.get(cacheKey);
  if (cachedResult) {
    console.log('Serving from cache.');
    return res.json({ result: cachedResult, cached: true });
  }

  const systemInstruction = `You are EchoReader, a highly sophisticated AI reading companion.
The user is reading aloud or pasting text from:
Book/Article Title: "${bookTitle}"
Author: "${bookAuthor}"
Reading Purpose: "${readingPurpose}"
Target Explanation Depth: "${depthLevel}"
Special Study Focus Area: "${focusArea}"

CURRENT MASTER NOTES:
${previousMasterNotes}

Analyze the text chunk and output ONLY valid JSON with these fields:
- summary (string)
- keyPoints (list of strings)
- connections (list of strings)
- reflections (list of strings)
- webResearch (list of strings)
- vocabulary (list of arrays, e.g. [["word", "definition"]])
- questions (list of strings)
- masterNotesSuggestedUpdate (string)
- flashcards (list of objects with 'question' and 'answer')`;

  try {
    const openai = getOpenAIClient(req.apiKey);
    const response = await openai.chat.completions.create({
      model: modelId || 'llama-3.1-8b-instant',
      response_format: { type: "json_object" },
      messages: [
        { role: 'system', content: systemInstruction },
        { role: 'user', content: `Here is the read text chunk:\n\n${chunkText}` }
      ],
      temperature: 0.4
    });

    const content = response.choices[0].message.content;
    
    // Cache the result
    cache.set(cacheKey, content);
    
    res.json({ result: content, cached: false });
  } catch (error) {
    console.error('Error analyzing chunk:', error);
    res.status(500).json({ error: error.message });
  }
});

// 2. Voice Input Endpoint (Speech to Text using Whisper model)
app.post('/api/voice/transcribe', requireDynamicApiKey, upload.single('audio'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No audio file provided' });
    }
    
    const openai = getOpenAIClient(req.apiKey);
    // Call Whisper API (supported by Groq/LiteLLM)
    const transcription = await openai.audio.transcriptions.create({
      file: fs.createReadStream(req.file.path),
      model: 'whisper-large-v3', // Groq's supported whisper model
    });

    // Cleanup uploaded file after processing
    fs.unlinkSync(req.file.path);

    res.json({ text: transcription.text });
  } catch (error) {
    console.error('Error in transcription:', error);
    if (req.file) fs.unlinkSync(req.file.path);
    res.status(500).json({ error: error.message });
  }
});

// 3. Session Management / Raw Generative Endpoint
app.post('/api/generate', requireDynamicApiKey, async (req, res) => {
  const { prompt, modelId } = req.body;
  
  try {
    const openai = getOpenAIClient(req.apiKey);
    const response = await openai.chat.completions.create({
      model: modelId || 'llama-3.1-8b-instant',
      messages: [{ role: 'user', content: prompt }],
      temperature: 0.7
    });

    res.json({ result: response.choices[0].message.content });
  } catch (error) {
    console.error('Error generating response:', error);
    res.status(500).json({ error: error.message });
  }
});

app.listen(port, () => {
  console.log(`EchoReader Proxy Backend running on port ${port}`);
});
