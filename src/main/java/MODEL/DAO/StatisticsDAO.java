package MODEL.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DBACCESS.DBConnection;
import MODEL.CLASS.ServiceStatistics;

public class StatisticsDAO {
    
    public int getTotalServices() throws SQLException {
        System.out.println("--- getTotalServices ---");
        String sql = "SELECT COUNT(*) AS total_services FROM service";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int count = rs.getInt("total_services");
                System.out.println("total_services" + count);
                return count;
            }
        }
        return 0;
    }

    public int getTotalBookings() throws SQLException {
        System.out.println("--- getTotalBookings ---");
        String sql = "SELECT COUNT(*) AS total_bookings FROM booking";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int count = rs.getInt("total_bookings");
                System.out.println("tot_bookings:" + count);
                return count;
            }
        }
        return 0;
    }

    public List<Map<String, Object>> getCategoryWiseServices() throws SQLException {
        System.out.println("--- getCategoryWiseServices ---");
        String sql = """
            SELECT 
                c.category_name,
                c.category_id, 
                COUNT(*) AS total_services 
            FROM service s
            JOIN category c ON s.category_id = c.category_id
            GROUP BY c.category_id, c.category_name
        """;
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("category_id", rs.getInt("category_id"));
                row.put("category_name", rs.getString("category_name"));
                row.put("total_services", rs.getInt("total_services"));
                results.add(row);
                System.out.println("Category: " + rs.getString("category_name") +
                                 ", Services: " + rs.getInt("total_services"));
            }
        }
        return results;
    }

    public List<ServiceStatistics> getMostBookedServices() throws SQLException {
        System.out.println("--- getMostBookedServices ---");
        String sql = """
            SELECT 
                s.service_id,
                s.service_name,
                COUNT(b.booking_id) AS total_bookings,
                COALESCE(SUM(p.product_price), 0) as total_revenue,
                CASE 
                    WHEN COUNT(b.booking_id) > 0 
                    THEN COALESCE(SUM(p.product_price), 0) / COUNT(b.booking_id)
                    ELSE 0 
                END as avg_revenue_per_booking
            FROM service s
            LEFT JOIN booking b ON s.service_id = b.service_id
            LEFT JOIN payment p ON b.booking_id = p.booking_id
            GROUP BY s.service_id, s.service_name
            ORDER BY total_bookings DESC
            LIMIT 5
        """;
        
        List<ServiceStatistics> statistics = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ServiceStatistics stat = new ServiceStatistics(
                    rs.getInt("service_id"),
                    rs.getString("service_name"),
                    rs.getDouble("total_revenue"),
                    rs.getInt("total_bookings"),
                    rs.getDouble("avg_revenue_per_booking"),
                    "all time"
                );
                statistics.add(stat);
                System.out.println("Service ID: " + stat.getServiceId() + 
                                 ", Bookings: " + stat.getTotalBookings());
            }
        }
        return statistics;
    }

    public List<Map<String, Object>> getBookingsByDate() throws SQLException {
        System.out.println("--- getBookingsByDate ---");
        String sql = """
            SELECT 
                booked_date,
                COUNT(*) AS total_bookings,
                COALESCE(SUM(p.product_price), 0) as total_revenue
            FROM booking b
            LEFT JOIN payment p ON b.booking_id = p.booking_id
            GROUP BY booked_date
            ORDER BY booked_date ASC
        """;
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("booked_date", rs.getDate("booked_date"));
                row.put("total_bookings", rs.getInt("total_bookings"));
                row.put("total_revenue", rs.getDouble("total_revenue"));
                results.add(row);
                System.out.println("Date: " + rs.getDate("booked_date") +
                                 ", Bookings: " + rs.getInt("total_bookings"));
            }
        }
        return results;
    }

    public List<ServiceStatistics> getMostProfitableServices(String timeframe) throws SQLException {
        System.out.println("--- getMostProfitableServices ---");
        LocalDate startDate = getStartDate(timeframe);
        LocalDate endDate = LocalDate.now();
        
        String sql = """
            SELECT 
                s.service_id,
                s.service_name,
                COUNT(DISTINCT b.booking_id) as total_bookings,
                COALESCE(SUM(p.product_price), 0) as total_revenue,
                CASE 
                    WHEN COUNT(DISTINCT b.booking_id) > 0 
                    THEN COALESCE(SUM(p.product_price), 0) / COUNT(DISTINCT b.booking_id)
                    ELSE 0 
                END as profit_per_booking
            FROM service s
            LEFT JOIN booking b ON s.service_id = b.service_id
            LEFT JOIN payment p ON b.booking_id = p.booking_id
            WHERE b.booked_date >= ? AND b.booked_date <= ?
            GROUP BY s.service_id, s.service_name
            HAVING COUNT(DISTINCT b.booking_id) > 0
            ORDER BY total_revenue DESC
            LIMIT 5
        """;
        
        return getServiceStatistics(sql, startDate, endDate, timeframe, "Most Profitable");
    }

    public List<ServiceStatistics> getLeastProfitableServices(String timeframe) throws SQLException {
        System.out.println("--- getLeastProfitableServices ---");
        LocalDate startDate = getStartDate(timeframe);
        LocalDate endDate = LocalDate.now();
        
        String sql = """
            SELECT 
                s.service_id,
                s.service_name,
                COUNT(DISTINCT b.booking_id) as total_bookings,
                COALESCE(SUM(p.product_price), 0) as total_revenue,
                CASE 
                    WHEN COUNT(DISTINCT b.booking_id) > 0 
                    THEN COALESCE(SUM(p.product_price), 0) / COUNT(DISTINCT b.booking_id)
                    ELSE 0 
                END as profit_per_booking
            FROM service s
            LEFT JOIN booking b ON s.service_id = b.service_id
            LEFT JOIN payment p ON b.booking_id = p.booking_id
            WHERE b.booked_date >= ? AND b.booked_date <= ?
            GROUP BY s.service_id, s.service_name
            HAVING COUNT(DISTINCT b.booking_id) > 0
            ORDER BY total_revenue ASC
            LIMIT 5
        """;
        
        System.out.println("Current timeframe selected: " + timeframe);
        return getServiceStatistics(sql, startDate, endDate, timeframe, "Least Profitable");
    }

    private List<ServiceStatistics> getServiceStatistics(String sql, LocalDate startDate, 
            LocalDate endDate, String timeframe, String type) throws SQLException {
        List<ServiceStatistics> statistics = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, java.sql.Date.valueOf(startDate));
            ps.setDate(2, java.sql.Date.valueOf(endDate));
            
            System.out.println(type + " query date range: " + startDate + " to " + endDate);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ServiceStatistics stat = new ServiceStatistics(
                        rs.getInt("service_id"),
                        rs.getString("service_name"),
                        rs.getDouble("total_revenue"),
                        rs.getInt("total_bookings"),
                        rs.getDouble("profit_per_booking"),
                        timeframe
                    );
                    statistics.add(stat);
                    System.out.println("Found " + type.toLowerCase() + " service: " + 
                        stat.getServiceName() + ", Revenue: " + stat.getTotalRevenue() + 
                        ", Bookings: " + stat.getTotalBookings());
                }
            }
        }
        
        System.out.println("Retrieved " + statistics.size() + " " + type.toLowerCase() + " services");
        return statistics;
    }

    public List<ServiceStatistics> getBookingsPerService(String timeframe) throws SQLException {
        System.out.println("--- getBookingsPerService ---");
        LocalDate startDate = getStartDate(timeframe);
        LocalDate endDate = LocalDate.now();
        
        String sql = """
            SELECT 
                s.service_id,
                s.service_name,
                COUNT(DISTINCT b.booking_id) as total_bookings,
                COALESCE(SUM(p.product_price), 0) as total_revenue,
                CASE 
                    WHEN COUNT(DISTINCT b.booking_id) > 0 
                    THEN COALESCE(SUM(p.product_price), 0) / COUNT(DISTINCT b.booking_id)
                    ELSE 0 
                END as avg_revenue_per_booking
            FROM service s
            LEFT JOIN booking b ON s.service_id = b.service_id AND b.booked_date >= ? AND b.booked_date <= ?
            LEFT JOIN payment p ON b.booking_id = p.booking_id
            GROUP BY s.service_id, s.service_name
            ORDER BY total_bookings DESC, s.service_name ASC
        """;

        List<ServiceStatistics> statistics = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, java.sql.Date.valueOf(startDate));
            ps.setDate(2, java.sql.Date.valueOf(endDate));
            
            System.out.println("Date range: " + startDate + " to " + endDate);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ServiceStatistics stat = new ServiceStatistics(
                        rs.getInt("service_id"),
                        rs.getString("service_name"),
                        rs.getDouble("total_revenue"),
                        rs.getInt("total_bookings"),
                        rs.getDouble("avg_revenue_per_booking"),
                        timeframe
                    );
                    statistics.add(stat);
                    System.out.println("Service: " + stat.getServiceName() + 
                                     ", Bookings: " + stat.getTotalBookings() +
                                     ", Revenue: " + stat.getTotalRevenue());
                }
            }
        }
        
        System.out.println("Retrieved booking stats for " + statistics.size() + " services");
        return statistics;
    }
    
    public List<ServiceStatistics> getServiceTrends() throws SQLException {
        System.out.println("--- getServiceTrends ---");
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
        
        String sql = """
            WITH CurrentMonth AS (
                SELECT 
                    s.service_id,
                    s.service_name,
                    COUNT(DISTINCT b.booking_id) as total_bookings,
                    COALESCE(SUM(p.product_price), 0) as total_revenue
                FROM service s
                LEFT JOIN booking b ON s.service_id = b.service_id
                LEFT JOIN payment p ON b.booking_id = p.booking_id
                WHERE b.booked_date >= ? AND b.booked_date < ?
                GROUP BY s.service_id, s.service_name
            ),
            PreviousMonth AS (
                SELECT 
                    s.service_id,
                    COUNT(DISTINCT b.booking_id) as prev_bookings,
                    COALESCE(SUM(p.product_price), 0) as prev_revenue
                FROM service s
                LEFT JOIN booking b ON s.service_id = b.service_id
                LEFT JOIN payment p ON b.booking_id = p.booking_id
                WHERE b.booked_date >= ? AND b.booked_date < ?
                GROUP BY s.service_id
            )
            SELECT 
                c.service_id,
                c.service_name,
                c.total_bookings,
                c.total_revenue,
                COALESCE(p.prev_bookings, 0) as prev_bookings,
                COALESCE(p.prev_revenue, 0) as prev_revenue,
                CASE 
                    WHEN COALESCE(p.prev_bookings, 0) = 0 THEN 100
                    ELSE ((c.total_bookings - p.prev_bookings) * 100.0 / p.prev_bookings)
                END as booking_growth,
                CASE 
                    WHEN COALESCE(p.prev_revenue, 0) = 0 THEN 100
                    ELSE ((c.total_revenue - p.prev_revenue) * 100.0 / p.prev_revenue)
                END as revenue_growth
            FROM CurrentMonth c
            LEFT JOIN PreviousMonth p ON c.service_id = p.service_id
            ORDER BY revenue_growth DESC
        """;
        
        List<ServiceStatistics> trends = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
                
            ps.setDate(1, java.sql.Date.valueOf(currentMonthStart));
            ps.setDate(2, java.sql.Date.valueOf(currentMonthStart.plusMonths(1)));
            ps.setDate(3, java.sql.Date.valueOf(previousMonthStart));
            ps.setDate(4, java.sql.Date.valueOf(currentMonthStart));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ServiceStatistics stat = new ServiceStatistics(
                        rs.getInt("service_id"),
                        rs.getString("service_name"),
                        rs.getDouble("total_revenue"),
                        rs.getInt("total_bookings"),
                        0, // Not using profit margin here
                        "current"
                    );
                    stat.setPreviousRevenue(rs.getDouble("prev_revenue"));
                    stat.setPreviousBookings(rs.getInt("prev_bookings"));
                    stat.setRevenueGrowth(rs.getDouble("revenue_growth"));
                    stat.setBookingGrowth(rs.getDouble("booking_growth"));
                    trends.add(stat);
                }
            }
        }
        return trends;
    }

    private LocalDate getStartDate(String timeframe) {
        LocalDate endDate = LocalDate.now();
        return switch (timeframe.toLowerCase()) {
            case "this month" -> endDate.withDayOfMonth(1);
            case "last month" -> endDate.minusMonths(1).withDayOfMonth(1);
            case "last 3 months" -> endDate.minusMonths(3);
            default -> endDate.minusMonths(1);
        };
    }
}