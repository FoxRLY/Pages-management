package com.mangareader.pagesmanagementservice.service;

import com.mangareader.pagesmanagementservice.controller.request.AddPageRequest;
import java.util.Map;

public interface PageService {
  void addPage(AddPageRequest request);
  Map<Long, String> getPages(String titleId, String chapterNumber, String pageNumber, String pageSize);
}
