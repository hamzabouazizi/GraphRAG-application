export async function sendChatMessage(question: string, token: string) {

  const response = await fetch(`${process.env.REACT_APP_CHAT_URL}/chat/`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ question }),
  });


  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Chat failed: ${error}`);
  }

  const data = await response.json();
  return data;
}

export function streamChatMessage({
  question,
  token,
  conversationId = undefined,
  topK = 5,
  alpha = 0.7,
  useMmr = true,
  onToken = () => { },
  onStart = () => { },
  onEnd = () => { },
  onError = () => { },
}: {
  question: string;
  token: string;
  conversationId?: string;
  topK?: number;
  alpha?: number;
  useMmr?: boolean;
  onToken?: (data: any) => void;
  onStart?: (data: any) => void;
  onEnd?: (data: any) => void;
  onError?: (data: any) => void;
} = {} as any) {
  // build URL with query params 
  const params = new URLSearchParams();
  params.set("question", question);
  if (conversationId) params.set("conversation_id", conversationId);
  params.set("top_k", String(topK));
  params.set("alpha", String(alpha));
  params.set("use_mmr", String(useMmr));
  if (token) params.set("token", token);

  //const url = `${process.env.REACT_APP_CHAT_URL.replace(/\/$/, "")}/chat/stream?${params.toString()}`;
  if (!process.env.REACT_APP_CHAT_URL) {
    throw new Error("REACT_APP_CHAT_URL is not defined");
  }
  const url = `${process.env.REACT_APP_CHAT_URL.replace(/\/$/, "")}/chat/stream?${params.toString()}`;


  const es = new EventSource(url);

  es.addEventListener("start", (e) => {
    try { onStart(e.data); } catch (err) { }
  });

  es.addEventListener("token", (e) => {
    try { onToken(e.data); } catch (err) { }
  });

  es.addEventListener("end", (e) => {
    try { onEnd(e.data); } catch (err) { }
    try { es.close(); } catch (err) { }
  });

  es.addEventListener("error", (e) => {
    try { onError(e); } catch (err) { }
    try { es.close(); } catch (err) { }
  });

  return {
    close: () => {
      try { es.close(); } catch (err) { }
    }
  };
}
