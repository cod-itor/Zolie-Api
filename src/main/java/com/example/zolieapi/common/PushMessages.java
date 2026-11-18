package com.example.zolieapi.common;

import com.example.zolieapi.model.enums.NotificationType;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PushMessages {

    private static final String KHMER = "km";

    private static final Map<NotificationType, String[]> BODIES = Map.ofEntries(
            Map.entry(NotificationType.FOLLOW, text("started following you", "បានចាប់ផ្តើមតាមដានអ្នក")),
            Map.entry(NotificationType.FOLLOW_REQUEST, text("asked to follow you", "បានស្នើសុំតាមដានអ្នក")),
            Map.entry(NotificationType.FOLLOW_ACCEPTED, text("accepted your follow request", "បានទទួលយកសំណើតាមដានរបស់អ្នក")),
            Map.entry(NotificationType.POST_LIKE, text("liked your post", "បានចូលចិត្តការបង្ហោះរបស់អ្នក")),
            Map.entry(NotificationType.REEL_LIKE, text("liked your Reel", "បានចូលចិត្តវីដេអូរបស់អ្នក")),
            Map.entry(NotificationType.COMMENT, text("commented", "បានផ្តល់មតិ")),
            Map.entry(NotificationType.COMMENT_LIKE, text("liked your comment", "បានចូលចិត្តមតិរបស់អ្នក")),
            Map.entry(NotificationType.EVENT_REMINDER, text("An event you saved is starting soon", "ព្រឹត្តិការណ៍ដែលអ្នករក្សាទុកជិតចាប់ផ្តើមហើយ")),
            Map.entry(NotificationType.MENTION, text("mentioned you", "បានលើកឈ្មោះអ្នក")),
            Map.entry(NotificationType.MATCH, text("You have a new match", "អ្នកមានការផ្គូផ្គងថ្មី")),
            Map.entry(NotificationType.DATING_INTRO, text("sent you an intro", "បានផ្ញើសារណែនាំខ្លួនមកអ្នក")),
            Map.entry(NotificationType.MESSAGE, text("sent you a message", "បានផ្ញើសារមកអ្នក")),
            Map.entry(NotificationType.STORY_REPLY, text("replied to your story", "បានឆ្លើយតបរឿងរ៉ាវរបស់អ្នក")),
            Map.entry(NotificationType.STICKER_RESPONSE, text("answered your sticker", "បានឆ្លើយស្ទីគ័ររបស់អ្នក")),
            Map.entry(NotificationType.COUNTDOWN_ENDED, text("A countdown has ended", "ការរាប់ថយក្រោយបានបញ្ចប់")),
            Map.entry(NotificationType.REPORT_RESOLVED, text("Your report was reviewed", "របាយការណ៍របស់អ្នកត្រូវបានពិនិត្យ")),
            Map.entry(NotificationType.MODERATION_NOTICE, text("A notice from the Zolie team", "ការជូនដំណឹងពីក្រុមការងារ Zolie")));

    public String body(NotificationType type, String locale) {
        String[] bodies = BODIES.get(type);
        return KHMER.equals(locale) ? bodies[1] : bodies[0];
    }

    private String[] text(String english, String khmer) {
        return new String[]{english, khmer};
    }
}
