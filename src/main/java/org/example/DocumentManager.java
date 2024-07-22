package org.example;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;


public class DocumentManager {

   private static final int MAX_GENERATOR_CAPACITY = 10;
   private static final int MIN_GENERATOR_CAPACITY = 0;
   private final Map<String, Document> storage = new HashMap<>();
   private final SearchHandler searchHandler = new SearchHandler();

   public Document save(Document document) {
       checkAndGenerateId(document);
       checkIdDuplicateId(document);
       document.setCreated(Instant.now());
       storage.put(document.getId(), document);
       return document;
   }

   public List<Document> search(SearchRequest request) {
       List<Document> results = new ArrayList<>();
       for (Document document : storage.values()) {
           if (searchHandler.matches(document, request)) {
               results.add(document);
           }
       }
       return results;
   }

   public Optional<Document> findById(String id) {
       validateId(id);
       return Optional.ofNullable(storage.get(id));

   }

   private void checkAndGenerateId(Document document) {
       if (document.getId() == null || document.getId().isBlank()) {
           String substring = UUID.randomUUID().toString().substring(MIN_GENERATOR_CAPACITY
                   , MAX_GENERATOR_CAPACITY);
           document.setId(substring);
       }
   }

   private void checkIdDuplicateId(Document document) {
       if (storage.containsKey(document.getId())) {
           throw new IllegalArgumentException("Id: " + document.getId()
                   + " is already exist");
       }
   }

   private void validateId(String id) {
       if (id == null || id.isBlank()) {
           throw new IllegalArgumentException("Please specify document id");
       }
   }

   private void checkStorageContent() {
       for (Map.Entry<String, Document> doc : storage.entrySet()) {
           System.out.println(doc);
       }
   }

   @Data
   @Builder
   public static class SearchRequest {
       private List<String> titlePrefixes;
       private List<String> containsContents;
       private List<String> authorIds;
       private Instant createdFrom;
       private Instant createdTo;
   }

   @Data
   @Builder
   public static class Document {
       private String id;
       private String title;
       private String content;
       private Author author;
       private Instant created;
   }

   @Data
   @Builder
   public static class Author {
       private String id;
       private String name;
   }

   interface SearchCriteriaHandler {
       boolean matches(Document document, SearchRequest request);
   }

   public static class TitlePrefixHandler implements SearchCriteriaHandler {
       @Override
       public boolean matches(Document document, SearchRequest request) {
           if (request.getTitlePrefixes() != null && !request.getTitlePrefixes().isEmpty()) {
               return request.getTitlePrefixes().stream()
                       .anyMatch(prefix -> document.getTitle() != null
                               && document.getTitle().startsWith(prefix));
           }
           return false;
       }
   }

   public static class ContentHandler implements SearchCriteriaHandler {
       @Override
       public boolean matches(Document document, SearchRequest request) {
           if (request.getContainsContents() != null && !request.getContainsContents().isEmpty()) {
               return request.getContainsContents().stream()
                       .anyMatch(content -> document.getContent() != null
                                       && document.getContent().contains(content));
           }
           return false;
       }
   }

   public static class AuthorIdHandler implements SearchCriteriaHandler {
       @Override
       public boolean matches(Document document, SearchRequest request) {
           if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
               return request.getAuthorIds().contains(
                       document.getAuthor() != null ? document.getAuthor().getId() : null);
           }
           return false;
       }
   }

   public static class CreatedFromHandler implements SearchCriteriaHandler {
       @Override
       public boolean matches(Document document, SearchRequest request) {
           if (request.getCreatedFrom() != null) {
               return !document.getCreated().isBefore(request.getCreatedFrom());
           }
           return false;
       }
   }

   public static class CreatedToHandler implements SearchCriteriaHandler {
       @Override
       public boolean matches(Document document, SearchRequest request) {
           if (request.getCreatedTo() != null) {
               return !document.getCreated().isAfter(request.getCreatedTo());
           }
           return false;
       }
   }

   public class SearchHandler {
       private final List<SearchCriteriaHandler> handlers;

       public SearchHandler() {
           handlers = List.of(
                   new TitlePrefixHandler(),
                   new ContentHandler(),
                   new AuthorIdHandler(),
                   new CreatedFromHandler(),
                   new CreatedToHandler()
           );
       }

       public boolean matches(Document document, SearchRequest request) {
           for (SearchCriteriaHandler handler : handlers) {
               if (handler.matches(document, request)) {
                   return true;
               }
           }
           return false;
       }
   }
}


