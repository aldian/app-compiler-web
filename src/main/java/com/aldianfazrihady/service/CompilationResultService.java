package com.aldianfazrihady.service;

import com.aldianfazrihady.model.CompilationResult;
import com.aldianfazrihady.repository.CompilationResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by AldianFazrihady on 11/15/15.
 */
@Service
@Transactional
public class CompilationResultService {
    @Autowired
    CompilationResultRepository compilationResultRepository;

    public CompilationResult update(CompilationResult compilationResult) {
        return compilationResultRepository.save(compilationResult);
    }

    public CompilationResult findById(long id) {
        return compilationResultRepository.findOne(id);
    }
}
