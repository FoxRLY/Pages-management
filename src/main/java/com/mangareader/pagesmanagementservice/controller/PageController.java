package com.mangareader.pagesmanagementservice.controller;

import com.mangareader.pagesmanagementservice.controller.request.AddPageRequest;
import com.mangareader.pagesmanagementservice.service.PageService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pages")
public class PageController {
  private final PageService pageService;

  @PostMapping
  public ResponseEntity<Void> addPage(AddPageRequest request) {
    pageService.addPage(request);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<Map<Long, String>> getPages(@RequestParam String titleId,
                                                    @RequestParam String chapterNumber,
                                                    @RequestParam String pageNumber,
                                                    @RequestParam String pageSize) {
    return ResponseEntity.ok(pageService.getPages(titleId, chapterNumber, pageNumber, pageSize));
  }
}
