package com.example.zolieapi.repository;

import lombok.experimental.UtilityClass;

@UtilityClass
public class VisibilitySql {

    public static final String AUTHOR_ACTIVE = " a.status = 'active' and a.deleted_at is null ";

    public static final String NOT_BLOCKED = """
             not exists (select 1 from blocks b
                 where (b.blocker_id = :viewer and b.blocked_id = a.id)
                    or (b.blocker_id = a.id and b.blocked_id = :viewer)) \
            """;

    public static final String FOLLOWS_AUTHOR = """
             exists (select 1 from follows f
                 where f.follower_id = :viewer and f.followed_id = a.id and f.status = 'accepted') \
            """;

    public static final String CAN_SEE_AUTHOR = AUTHOR_ACTIVE + " and " + NOT_BLOCKED
            + " and (a.id = :viewer or a.account_type = 'public' or " + FOLLOWS_AUTHOR + ") ";

    public static final String CAN_SEE_POST = CAN_SEE_AUTHOR
            + " and (p.visibility = 'public' or a.id = :viewer or " + FOLLOWS_AUTHOR + ") ";

    public static final String CAN_SEE_REEL = CAN_SEE_AUTHOR
            + " and (r.visibility = 'public' or a.id = :viewer or " + FOLLOWS_AUTHOR + ") ";
}
