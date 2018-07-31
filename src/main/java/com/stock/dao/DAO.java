package com.stock.dao;

import com.stock.entity.Symbol;
import com.stock.entity.TimeSeries;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

public class DAO {
    private static Session session;
    private static ServiceRegistry serviceRegistry;
    private static SessionFactory sessionFactory;

    public static Session getSession() {
        if (session == null) {
            Configuration configuration = new Configuration();
            configuration.addAnnotatedClass(TimeSeries.class);
            configuration.addAnnotatedClass(Symbol.class);
            configuration.configure();

            serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
                    configuration.getProperties()).build();

            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
            session = sessionFactory.openSession();
        }
        return session;
    }

    public static void close() {
        getSession().close();
        session = null;
        sessionFactory.close();
        StandardServiceRegistryBuilder.destroy(serviceRegistry);
    }
}
