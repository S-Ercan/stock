package com.stock.controller;

import com.stock.analyze.TrendCalculator;
import com.stock.dao.DAO;
import com.stock.entity.TimeSeries;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

@RestController
public class Controller {
    private Logger log = LoggerFactory.getLogger(Controller.class);

    @RequestMapping("/test")
    @CrossOrigin("*")
    public ResponseEntity<List<TimeSeries>> test() {
        Query<TimeSeries> query = DAO.getSession().createQuery("FROM TimeSeries ORDER BY trading_day DESC").setMaxResults(100);
        List<TimeSeries> result = query.list();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/trend", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin("*")
    public ResponseEntity<List<HashMap<String, List<HashMap<String, Object>>>>> trend() {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.add(Calendar.DATE, -60);
        List<HashMap<String, List<HashMap<String, Object>>>> output = new TrendCalculator().calculateTrends(today.getTime());
        return new ResponseEntity<>(output, HttpStatus.OK);
    }
}
