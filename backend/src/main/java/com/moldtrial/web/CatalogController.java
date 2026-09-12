package com.moldtrial.web;

import com.moldtrial.api.Dtos.*;
import com.moldtrial.repo.Repositories.*;
import com.moldtrial.service.Mapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final MoldRevisionRepository molds;
    private final CavityRepository cavities;
    private final ParameterGroupRepository params;
    private final BatchRepository batches;
    private final Mapper mapper;

    public CatalogController(MoldRevisionRepository molds, CavityRepository cavities,
                             ParameterGroupRepository params, BatchRepository batches, Mapper mapper) {
        this.molds = molds;
        this.cavities = cavities;
        this.params = params;
        this.batches = batches;
        this.mapper = mapper;
    }

    @GetMapping("/molds")
    public List<MoldRevisionDto> molds() {
        return molds.findAll().stream().map(mapper::mold).toList();
    }

    @GetMapping("/parameters")
    public List<ParameterGroupDto> parameters() {
        return params.findAll().stream().map(mapper::param).toList();
    }

    @GetMapping("/batches/simple")
    public List<BatchSimple> simpleBatches() {
        return batches.findAllByOrderByTrialAtAsc().stream()
                .map(b -> new BatchSimple(b.getId(), b.getCode(), b.getMoldRevision().getRevision()))
                .toList();
    }

    public record BatchSimple(Long id, String code, String moldRevision) {}
}
