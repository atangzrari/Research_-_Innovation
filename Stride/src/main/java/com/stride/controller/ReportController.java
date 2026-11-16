package com.stride.controller;

import com.stride.report.Report;
import com.stride.report.ReportService;
import com.stride.service.ServiceException;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Management reports RP01-RP05 with charts and CSV export (lecturers and administrators only). */
@WebServlet(name = "reportController", urlPatterns = {"/reports", "/report"})
public class ReportController extends BaseController {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/reports".equals(request.getServletPath())) {
            request.setAttribute("catalogue", ReportService.CATALOGUE);
            render(request, response, "reports");
            return;
        }
        Report report;
        try {
            report = app().reports.build(param(request, "id"), user(request));
        } catch (ServiceException e) {
            notFound(response);
            return;
        }
        if ("csv".equals(request.getParameter("format"))) {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"stride-" + report.getId() + "-"
                    + app().now().toLocalDate() + ".csv\"");
            // A byte-order mark so that spreadsheet programs read the file as UTF-8.
            response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            response.getOutputStream().write(report.toCsv().getBytes(StandardCharsets.UTF_8));
            return;
        }
        request.setAttribute("report", report);
        request.setAttribute("catalogue", ReportService.CATALOGUE);
        render(request, response, "report");
    }
}
