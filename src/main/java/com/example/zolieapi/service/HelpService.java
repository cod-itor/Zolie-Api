package com.example.zolieapi.service;

import com.example.zolieapi.dto.response.HelpArticleResponse;
import java.util.List;

public interface HelpService {

    List<HelpArticleResponse> list(String topic);
}
