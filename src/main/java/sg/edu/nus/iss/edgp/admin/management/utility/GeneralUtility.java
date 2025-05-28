package sg.edu.nus.iss.edgp.admin.management.utility;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class GeneralUtility {

	public static String makeNotNull(Object str) {
		if (str == null) {
			return "";
		} else if (str.equals("null")) {
			return "";
		} else {
			return str.toString();
		}
	}
	public static boolean isGetWithBody(HttpServletRequest request) {
	    return "GET".equals(request.getMethod()) && request.getContentLength() > 0;
	}

}