package com.stock.dao;

import com.stock.entity.TimeSeries;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DAO {
    private static Session session;
    private static SessionFactory sessionFactory;

    public static Session getSession() {
        if (session == null) {
            Configuration configuration = new Configuration();
            configuration.addAnnotatedClass(TimeSeries.class);
            sessionFactory = configuration.configure().buildSessionFactory();
            session = sessionFactory.openSession();
        }
        return session;
    }

    public static void close() {
        getSession().close();
    }
}
