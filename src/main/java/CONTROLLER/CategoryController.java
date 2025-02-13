package CONTROLLER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import MODEL.CLASS.Category;
import MODEL.CLASS.Service;
import MODEL.DAO.CategoryDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import utils.sessionUtils;

public class CategoryController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final CategoryDAO categoryDAO;

    public CategoryController() {
        this.categoryDAO = new CategoryDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Check authentication and authorization
        if (!isAuthorized(request, response)) {
            return;
        }
        
        response.sendRedirect(request.getContextPath() + "/pages/editServiceCategory.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Check authentication and authorization
        if (!isAuthorized(request, response)) {
            return;
        }

        String action = request.getPathInfo();
        if (action == null) {
            action = "/create"; // Default to create if no action specified
        }

        HttpSession session = request.getSession(false);
        
        try {
            switch (action) {
                case "/create":
                    handleCreate(request, session);
                    request.setAttribute("successMessage", session.getAttribute("successMessage"));
                    session.removeAttribute("successMessage");
                    break;
                case "/delete":
                    handleDelete(request, session);
                    request.setAttribute("successMessage", session.getAttribute("successMessage"));
                    request.setAttribute("errorMessage", session.getAttribute("errorMessage"));
                    session.removeAttribute("successMessage");
                    session.removeAttribute("errorMessage");
                    break;
                default:
                    request.setAttribute("errorMessage", "Invalid action specified");
                    break;
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        // Forward back to the edit page to show messages
        request.getRequestDispatcher("/pages/editServiceCategory.jsp").forward(request, response);
    }

    private void handleCreate(HttpServletRequest request, HttpSession session) throws Exception {
        String categoryName = request.getParameter("categoryName");
        String categoryDescription = request.getParameter("categoryDescription");

        // Create category using DAO
        int generatedId = categoryDAO.createCategory(categoryName, categoryDescription);

        // Update session category-service map
        updateSessionCategoryMapForCreate(session, generatedId, categoryName, categoryDescription);

        session.setAttribute("successMessage", "Category added successfully!");
    }

    private void handleDelete(HttpServletRequest request, HttpSession session) throws Exception {
        String categoryId = request.getParameter("categoryId");
        
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            boolean deleted = categoryDAO.deleteCategory(Integer.parseInt(categoryId));

            if (deleted) {
                // Update session category-service map
                updateSessionCategoryMapForDelete(session, Integer.parseInt(categoryId));
                session.setAttribute("successMessage", "Category deleted successfully!");
            } else {
                session.setAttribute("errorMessage", "Category not found or could not be deleted.");
            }
        } else {
            session.setAttribute("errorMessage", "Invalid category ID provided.");
        }
    }

    private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Check if user is logged in
        if (!sessionUtils.isLoggedIn(request, "isLoggedIn")) {
            request.setAttribute("error", "You must log in first.");
            request.getRequestDispatcher("/pages/login.jsp").forward(request, response);
            return false;
        }

        // Check if user is admin
        if (!sessionUtils.isAdmin(request)) {
            response.sendRedirect(request.getContextPath() + "/pages/forbidden.jsp");
            return false;
        }

        return true;
    }

    private void updateSessionCategoryMapForCreate(HttpSession session, int generatedId, 
            String categoryName, String categoryDescription) {
        @SuppressWarnings("unchecked")
        Map<Category, List<Service>> sessionCategoryServiceMap = 
            (Map<Category, List<Service>>) session.getAttribute("categoryServiceMap");

        if (sessionCategoryServiceMap == null) {
            sessionCategoryServiceMap = new HashMap<>();
        }

        Category newCategory = new Category(generatedId, categoryName, categoryDescription);
        sessionCategoryServiceMap.put(newCategory, new ArrayList<>());
        session.setAttribute("categoryServiceMap", sessionCategoryServiceMap);
    }

    private void updateSessionCategoryMapForDelete(HttpSession session, int categoryId) {
        @SuppressWarnings("unchecked")
        Map<Category, List<Service>> sessionCategoryServiceMap = 
                (Map<Category, List<Service>>) session.getAttribute("categoryServiceMap");

        if (sessionCategoryServiceMap != null) {
            Category toRemove = null;
            for (Category cat : sessionCategoryServiceMap.keySet()) {
                if (cat.getId() == categoryId) {
                    toRemove = cat;
                    break;
                }
            }

            if (toRemove != null) {
                sessionCategoryServiceMap.remove(toRemove);
                session.setAttribute("categoryServiceMap", sessionCategoryServiceMap);
            }
        }
    }
}