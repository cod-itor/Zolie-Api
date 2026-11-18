package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.SettingsResponse;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountType;
import org.springframework.stereotype.Component;

@Component
public class SettingsMapper {

    public SettingsResponse toResponse(User user) {
        return new SettingsResponse(
                user.getAccountType() == AccountType.PRIVATE,
                user.isShowOnlineStatus(),
                user.isReadReceipts(),
                user.isDataSaver(),
                user.isHideDatingFromFollowing(),
                Enums.value(user.getWhoCanMessage()),
                Enums.value(user.getWhoCanMention()),
                Enums.value(user.getStoryRepliesFrom()),
                user.getTheme(),
                user.getLocale());
    }
}
