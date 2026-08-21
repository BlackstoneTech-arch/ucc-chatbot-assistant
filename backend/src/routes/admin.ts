import { Router } from 'express';
import {
  getAnalytics, getConversations, getConversationMessages,
  updateDocument, deleteDocument, getDocuments, createFAQ,
  getKnowledgeBaseStats,
} from '../controllers/adminController.ts';
import { authenticate, authorize } from '../middleware/auth.ts';

const router = Router();

router.use(authenticate);
router.use(authorize('admin', 'superadmin'));

router.get('/analytics', getAnalytics);
router.get('/conversations', getConversations);
router.get('/conversations/:id/messages', getConversationMessages);
router.get('/documents', getDocuments);
router.put('/documents/:id', updateDocument);
router.delete('/documents/:id', deleteDocument);
router.post('/faqs', createFAQ);
router.get('/stats', getKnowledgeBaseStats);

export default router;
