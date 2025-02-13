package CONTROLLER;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import MODEL.CLASS.Category;
import MODEL.CLASS.Service;
import MODEL.DAO.ServiceDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import utils.CloudinaryUtil;
import utils.sessionUtils;

public class ServiceController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ServiceDAO serviceDAO;

    public ServiceController() {
        this.serviceDAO = new ServiceDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (!isAuthorized(request, response)) {
                return;
            }

            String categoryId = request.getParameter("categoryId");
            if (categoryId == null || categoryId.trim().isEmpty()) {
                request.setAttribute("error", "Invalid category ID.");
                request.getRequestDispatcher("/pages/error.jsp").forward(request, response);
                return;
            }

            List<Service> services = serviceDAO.getServicesByCategory(Integer.parseInt(categoryId));
            request.setAttribute("services", services);
            request.setAttribute("categoryId", categoryId);
            request.getRequestDispatcher("/pages/editService.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            request.setAttribute("error", "Invalid category ID format");
            request.getRequestDispatcher("/pages/error.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("error", "Error processing request: " + e.getMessage());
            request.getRequestDispatcher("/pages/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAuthorized(request, response)) {
            return;
        }

        String action = request.getPathInfo();
        if (action == null) {
            action = "";
        }

        HttpSession session = request.getSession(false);
        String categoryId = request.getParameter("categoryId");
        
        try {
            switch (action) {
                case "/create":
                    handleCreate(request, session);
                    break;
                case "/update":
                    handleUpdate(request, session);
                    break;
                case "/delete":
                    handleDelete(request, session);
                    break;
                default:
                    session.setAttribute("errorMessage", "Invalid action specified");
                    break;
            }
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        // Improved redirect logic
        if (categoryId != null) {
            response.sendRedirect(request.getContextPath() + "/pages/editService.jsp?categoryId=" + categoryId);
        } else {
            response.sendRedirect(request.getContextPath() + "/pages/error.jsp");
        }
    }

    private void handleCreate(HttpServletRequest request, HttpSession session) throws Exception {
        String serviceName = request.getParameter("serviceName");
        String categoryIdStr = request.getParameter("categoryId");
        String servicePriceStr = request.getParameter("servicePrice");
        String serviceDurationStr = request.getParameter("serviceDuration");
        String description = request.getParameter("serviceDescription");

        // Handle image upload
        Part filePart = request.getPart("serviceImage");
        String imageUrl = null;
        
        if (filePart != null && filePart.getSize() > 0) {
            try (InputStream inputStream = filePart.getInputStream()) {
                byte[] imageData = inputStream.readAllBytes();
                // Upload to Cloudinary
                imageUrl = CloudinaryUtil.uploadImage(imageData, "services");
            }
        }

        try {
            int categoryId = Integer.parseInt(categoryIdStr);
            double price = Double.parseDouble(servicePriceStr);
            int duration = Integer.parseInt(serviceDurationStr);
            int statusId = 4; // Default status

            int generatedId = serviceDAO.createService(
                serviceName, 
                categoryId, 
                price, 
                duration, 
                description, 
                statusId, 
                imageUrl
            );

            if (generatedId > 0) {
                updateSessionServiceMap(session, categoryId);
                session.setAttribute("successMessage", "Service added successfully!");
            } else {
                session.setAttribute("errorMessage", "Failed to add service");
            }
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Error creating service: " + e.getMessage());
            throw e;
        }
    }

    private void handleUpdate(HttpServletRequest request, HttpSession session) throws Exception {
        String serviceIdStr = request.getParameter("serviceId");
        String serviceName = request.getParameter("serviceName");
        String servicePriceStr = request.getParameter("servicePrice");
        String serviceDurationStr = request.getParameter("serviceDuration");
        String description = request.getParameter("serviceDescription");
        String categoryIdStr = request.getParameter("categoryId");
        String currentImageUrl = request.getParameter("currentImageUrl");

        try {
            int serviceId = Integer.parseInt(serviceIdStr);
            double price = Double.parseDouble(servicePriceStr);
            int duration = Integer.parseInt(serviceDurationStr);
            int categoryId = Integer.parseInt(categoryIdStr);

            // Handle image update
            Part filePart = request.getPart("serviceImage");
            String newImageUrl = currentImageUrl;

            if (filePart != null && filePart.getSize() > 0) {
                // Delete old image from Cloudinary if it exists
                if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                    String publicId = CloudinaryUtil.getPublicIdFromUrl(currentImageUrl);
                    CloudinaryUtil.deleteImage(publicId);
                }

                // Upload new image
                try (InputStream inputStream = filePart.getInputStream()) {
                    byte[] imageData = inputStream.readAllBytes();
                    newImageUrl = CloudinaryUtil.uploadImage(imageData, "services");
                }
            }

            boolean updated = serviceDAO.updateService(
                serviceId, 
                serviceName, 
                price, 
                duration, 
                description, 
                newImageUrl
            );

            if (updated) {
                updateSessionServiceMap(session, categoryId);
                session.setAttribute("successMessage", "Service updated successfully!");
            } else {
                session.setAttribute("errorMessage", "Failed to update service");
            }
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Error updating service: " + e.getMessage());
            throw e;
        }
    }

    private void handleDelete(HttpServletRequest request, HttpSession session) throws Exception {
        int serviceId = Integer.parseInt(request.getParameter("serviceId"));
        int categoryId = Integer.parseInt(request.getParameter("categoryId"));

        boolean deleted = serviceDAO.deleteService(serviceId);

        if (deleted) {
            updateSessionServiceMap(session, categoryId);
            session.setAttribute("successMessage", "Service deleted successfully!");
        } else {
            session.setAttribute("errorMessage", "Service not found or could not be deleted.");
        }
    }

    private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        if (!sessionUtils.isLoggedIn(request, "isLoggedIn")) {
            request.setAttribute("error", "You must log in first.");
            request.getRequestDispatcher("/pages/login.jsp").forward(request, response);
            return false;
        }

        if (!sessionUtils.isAdmin(request)) {
            response.sendRedirect(request.getContextPath() + "/pages/forbidden.jsp");
            return false;
        }

        return true;
    }

    private void updateSessionServiceMap(HttpSession session, int categoryId) throws Exception {
        @SuppressWarnings("unchecked")
        Map<Category, List<Service>> sessionCategoryServiceMap = 
                (Map<Category, List<Service>>) session.getAttribute("categoryServiceMap");

        if (sessionCategoryServiceMap != null) {
            List<Service> updatedServices = serviceDAO.getServicesByCategory(categoryId);
            
            for (Map.Entry<Category, List<Service>> entry : sessionCategoryServiceMap.entrySet()) {
                if (entry.getKey().getId() == categoryId) {
                    entry.setValue(updatedServices);
                    break;
                }
            }
            
            session.setAttribute("categoryServiceMap", sessionCategoryServiceMap);
        }
    }
}