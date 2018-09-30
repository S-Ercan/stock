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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

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
        List<HashMap<String, List<HashMap<String, Object>>>> output = new TrendCalculator().calculate();
        return new ResponseEntity<>(output, HttpStatus.OK);
    }

    @RequestMapping(value = "/trendFromDaysAgo", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin("*")
    public ResponseEntity<List<HashMap<String, List<HashMap<String, Object>>>>> trendFromDaysAgo(@RequestParam("daysAgo") int daysAgo) {
        List<HashMap<String, List<HashMap<String, Object>>>> output = new TrendCalculator().calculateFromStartDate(daysAgo);
        return new ResponseEntity<>(output, HttpStatus.OK);
    }

    @RequestMapping(value = "/trendWithNumberOfClusters", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin("*")
    public ResponseEntity<List<HashMap<String, List<HashMap<String, Object>>>>> trendWithNumberOfClusters(@RequestParam("numberOfClusters") int numberOfClusters) {
        List<HashMap<String, List<HashMap<String, Object>>>> output = new TrendCalculator().calculateWithNumberOfClusters(numberOfClusters);
        return new ResponseEntity<>(output, HttpStatus.OK);
    }

    @RequestMapping(value = "/trendFromDaysAgoAndWithNumberOfClusters", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin("*")
    public ResponseEntity<List<HashMap<String, List<HashMap<String, Object>>>>> trendFromDaysAgoAndWithNumberOfClusters(@RequestParam("daysAgo") int daysAgo, @RequestParam("numberOfClusters") int numberOfClusters) {
        List<HashMap<String, List<HashMap<String, Object>>>> output = new TrendCalculator().calculateFromStartDateAndWithNumberOfClusters(daysAgo, numberOfClusters);
        return new ResponseEntity<>(output, HttpStatus.OK);
    }
}
