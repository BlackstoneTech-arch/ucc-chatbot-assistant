import { Router } from 'express';
import { chatController, getProgrammes, getProgrammeById, getFAQs, getContacts, submitFeedback } from '../controllers/chatController.ts';

const router = Router();

router.post('/chat', chatController);
router.get('/programmes', getProgrammes);
router.get('/programmes/:id', getProgrammeById);
router.get('/faqs', getFAQs);
router.get('/contacts', getContacts);
router.post('/feedback', submitFeedback);

export default router;
