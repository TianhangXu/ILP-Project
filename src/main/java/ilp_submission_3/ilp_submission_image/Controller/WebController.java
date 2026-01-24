package ilp_submission_3.ilp_submission_image.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving the React frontend.`
 * Handles SPA routing by forwarding all non-API requests to index.html
 */
@Controller
public class WebController {

    /**
     * Serve React app for all routes except API endpoints
     */
    @GetMapping(value = {
            "/",
            "/orders/**",
            "/map/**",
            "/monitor/**"
    })
    public String index() {
        return "forward:/index.html";
    }
}
