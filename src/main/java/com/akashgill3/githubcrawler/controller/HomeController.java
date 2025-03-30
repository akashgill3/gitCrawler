package com.akashgill3.githubcrawler.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.akashgill3.githubcrawler.github.model.Practise;
import com.akashgill3.githubcrawler.github.model.Principle;
import com.akashgill3.githubcrawler.github.service.PrincipleCache;

@Controller
@RequestMapping("/")
@CrossOrigin(maxAge = 3600)
public class HomeController {

    private final PrincipleCache principleCache;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public HomeController(PrincipleCache principleCache) {
        this.principleCache = principleCache;
        this.markdownParser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    @GetMapping
    public String home(Model model) {
        Map<String, Principle> principles = getSortedPrinciples();
        model.addAttribute("principles", principles);
        return "home";
    }

    @GetMapping("/tree")
    public String tree(Model model) {
        Map<String, Principle> principles = getSortedPrinciples();
        model.addAttribute("principles", principles);
        return "tree";
    }
    
    @GetMapping("/api/content/principle/{name}")
    public String getPrincipleContent(@PathVariable String name, Model model) {
        Principle principle = principleCache.get(name);
        if (principle == null) {
            return "fragments/not-found";
        }
        
        model.addAttribute("name", principle.metadata().name());
        model.addAttribute("owner", principle.metadata().owner());
        model.addAttribute("value", principle.metadata().value());
        model.addAttribute("content", renderMarkdown(principle.content()));
        model.addAttribute("type", "principle");
        
        return "fragments/content";
    }
    
    @GetMapping("/api/content/practice/{principleName}/{practiceName}")
    public String getPracticeContent(
            @PathVariable String principleName, 
            @PathVariable String practiceName,
            Model model) {
        Principle principle = principleCache.get(principleName);
        if (principle == null || !principle.practises().containsKey(practiceName)) {
            return "fragments/not-found";
        }
        
        Practise practice = principle.practises().get(practiceName);
        model.addAttribute("name", practice.metadata().name());
        model.addAttribute("owner", practice.metadata().owner());
        model.addAttribute("metrics", practice.metadata().metrics());
        model.addAttribute("content", renderMarkdown(practice.content()));
        model.addAttribute("type", "practice");
        
        return "fragments/content";
    }
    
    @GetMapping("/api/content/subpractice/{principleName}/{practiceName}/{subPracticeName}")
    public String getSubPracticeContent(
            @PathVariable String principleName, 
            @PathVariable String practiceName,
            @PathVariable String subPracticeName,
            Model model) {
        Principle principle = principleCache.get(principleName);
        if (principle == null || 
            !principle.practises().containsKey(practiceName) ||
            !principle.practises().get(practiceName).subPractises().containsKey(subPracticeName)) {
            return "fragments/not-found";
        }
        
        Practise subPractice = principle.practises().get(practiceName).subPractises().get(subPracticeName);
        model.addAttribute("name", subPractice.metadata().name());
        model.addAttribute("owner", subPractice.metadata().owner());
        model.addAttribute("metrics", subPractice.metadata().metrics());
        model.addAttribute("content", renderMarkdown(subPractice.content()));
        model.addAttribute("type", "subpractice");
        
        return "fragments/content";
    }
    
    private String renderMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        Node document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }
    
    private Map<String, Principle> getSortedPrinciples() {
        // Get all principles and sort them by name
        return principleCache.getAll().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}
