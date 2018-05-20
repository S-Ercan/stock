package com.stock.controller;

import com.stock.dao.DAO;
import com.stock.entity.TimeSeries;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class Controller {
    private Logger log = LoggerFactory.getLogger(Controller.class);

    @RequestMapping("/test")
    @CrossOrigin("*")
    public ResponseEntity<List<TimeSeries>> test() {
        Query<TimeSeries> query = DAO.getSession().createQuery("FROM TimeSeries ORDER BY trading_day DESC").setMaxResults(10);
        List<TimeSeries> result = query.list();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
