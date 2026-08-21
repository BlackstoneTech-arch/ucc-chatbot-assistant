export interface Message {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

export interface AIResponse {
  content: string;
  sources: Array<{ title: string; url?: string; type: string }>;
  intent?: string;
  confidence?: number;
  escalated?: boolean;
}

export class AIService {
  private apiKey: string;
  private apiUrl: string;
  private model: string;

  constructor() {
    this.apiKey = process.env.AI_API_KEY || '';
    this.apiUrl = process.env.AI_API_URL || 'https://api.openai.com/v1';
    this.model = process.env.AI_MODEL || 'gpt-4o-mini';
  }

  async generateResponse(messages: Message[], systemPrompt: string, context?: string): Promise<AIResponse> {
    if (!this.apiKey) {
      return {
        content: 'AI service is not configured. Please contact the system administrator.',
        sources: [],
        escalated: true,
      };
    }

    const fullMessages: Message[] = [
      { role: 'system', content: systemPrompt },
      ...(context ? [{ role: 'system', content: `UCC Knowledge Base Context:\n${context}` }] : []),
      ...messages,
    ];

    try {
      const response = await fetch(`${this.apiUrl}/chat/completions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`,
        },
        body: JSON.stringify({
          model: this.model,
          messages: fullMessages,
          temperature: 0.3,
          max_tokens: 1000,
          top_p: 0.9,
        }),
      });

      if (!response.ok) {
        const error = await response.text();
        console.error('AI API error:', error);
        return {
          content: 'I\'m temporarily unable to generate a response. Please try again or contact UCC directly.',
          sources: [],
          escalated: true,
        };
      }

      const data = await response.json();
      const content = data.choices[0]?.message?.content || 'I couldn\'t generate a response.';

      return {
        content,
        sources: [],
        confidence: 0.8,
      };
    } catch (error) {
      console.error('AI generation error:', error);
      return {
        content: 'I\'m experiencing technical difficulties. Please try again later or contact UCC directly.',
        sources: [],
        escalated: true,
      };
    }
  }

  async classifyIntent(question: string): Promise<{ intent: string; confidence: number; entities: Record<string, any> }> {
    const systemPrompt = `You are an intent classifier for the UCC Chatbot Assistant. Classify the user's question into one of these intents:
- greeting
- about_ucc
- admission
- application_process
- application_deadline
- application_fee
- entry_requirements
- programme_information
- course_information
- course_duration
- tuition_fee
- other_fees
- payment_information
- registration
- academic_calendar
- examination
- graduation
- accommodation
- student_services
- ict_support
- professional_training
- software_services
- it_infrastructure
- consulting
- campus_information
- contact_information
- news
- events
- complaint
- feedback
- technical_problem
- human_support
- unknown

Extract entities like: programme, course, course_code, campus, academic_year, intake, fee_type, department, date.
Respond with ONLY valid JSON: {"intent": "...", "confidence": 0.95, "entities": {}}`;

    try {
      const response = await fetch(`${this.apiUrl}/chat/completions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`,
        },
        body: JSON.stringify({
          model: this.model,
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: question },
          ],
          temperature: 0,
          max_tokens: 200,
        }),
      });

      if (!response.ok) {
        return { intent: 'unknown', confidence: 0, entities: {} };
      }

      const data = await response.json();
      const content = data.choices[0]?.message?.content || '{}';

      try {
        const parsed = JSON.parse(content);
        return {
          intent: parsed.intent || 'unknown',
          confidence: parsed.confidence || 0,
          entities: parsed.entities || {},
        };
      } catch {
        return { intent: 'unknown', confidence: 0, entities: {} };
      }
    } catch (error) {
      console.error('Intent classification error:', error);
      return { intent: 'unknown', confidence: 0, entities: {} };
    }
  }
}
