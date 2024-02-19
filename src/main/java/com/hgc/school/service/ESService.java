package com.hgc.school.service;

import com.hgc.school.dto.InfoWithCommentsDTO;

import java.util.List;

/**
 *
 */
public interface ESService {

    List<InfoWithCommentsDTO> search(String keyword);
}
