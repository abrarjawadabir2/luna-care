import type { FastifyRequest, FastifyReply } from 'fastify';
import { nearbySearchSchema } from '../schemas/index.js';

const NEARBY_CATEGORIES = [
  { id: 'pharmacy', label: 'Pharmacy / Dispensary', query: 'pharmacy', emoji: '💊' },
  { id: 'clinic', label: 'Clinic / Gynecologist', query: 'gynecologist clinic', emoji: '🏥' },
  { id: 'pads', label: 'Sanitary Pads', query: 'sanitary pads store', emoji: '🛒' },
  { id: 'cup', label: 'Menstrual Cup', query: 'menstrual cup store', emoji: '🌙' },
  { id: 'period_panties', label: 'Period Panties', query: 'period panties store', emoji: '🌸' },
  { id: 'hot_water_bag', label: 'Hot Water Bag / Heating Pad', query: 'heating pad pharmacy', emoji: '🔥' }
];

export class LocationController {
  static async getCategories(_request: FastifyRequest, reply: FastifyReply) {
    return reply.status(200).send({
      categories: NEARBY_CATEGORIES,
      privacyPolicy: 'Exact GPS coordinates are NEVER stored on LunaCare servers.'
    });
  }

  static async searchUrl(request: FastifyRequest, reply: FastifyReply) {
    const data = nearbySearchSchema.parse(request.body);
    const cat = NEARBY_CATEGORIES.find(c => c.id === data.category) || { query: data.category };

    let fullQuery = cat.query;
    if (data.city) {
      fullQuery += ` in ${data.city}`;
    } else if (data.country) {
      fullQuery += ` in ${data.country}`;
    }

    const encoded = encodeURIComponent(fullQuery);
    const mapsUrl = `https://www.google.com/maps/search/${encoded}`;

    return reply.status(200).send({
      category: data.category,
      mapsUrl,
      privacyNote: 'No location coordinates stored.'
    });
  }
}
