package bookstore.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import bookstore.service.model.BookDetail;
import bookstore.service.model.BuyDetail;
import bookstore.service.model.CustomerDetail;
import bookstore.service.model.OrderDetail;
import bookstore.service.model.Response;
import bookstore.service.model.SignIn;
import bookstore.service.model.Status;
import bookstore.service.rest.BookStoreService;

/**
 * 
 * @author Anurak Sirivoravit
 *
 */
@Controller
@SessionAttributes("maps")
public class HomeController {
	@Autowired
	private BookStoreService bookStoreService;

	/*
	 * Add user in model attribute
	 */
	@ModelAttribute("maps")
	public Map setupMaps() {
		Map map = new HashMap();
		map.put("customerDetail", new CustomerDetail());
		return map;
	}

	/**
	 * Mapping for home path
	 * 
	 * @param model
	 * @return
	 */
	@GetMapping("/")
	public String home(Model model) {
		// list random book from book service
		Response<List<BookDetail>> response = bookStoreService.getBookService().getRandomBooks();

		if (Status.SUCCESS.equalsIgnoreCase(response.getStatus())) {
			model.addAttribute("bookDetailList", response.getResult());
		} else {
			model.addAttribute("error", response.getMessage());
		}

		return "home";
	}

	/**
	 * mapping when click on buy button
	 * 
	 * @param buyForm
	 * @param sessionMap
	 * @return
	 */
	@PostMapping("/")
	public ModelAndView buy(BuyDetail buyForm, @ModelAttribute("maps") Map sessionMap) {
		CustomerDetail customerDetail = (CustomerDetail) sessionMap.get("customerDetail");

		// for reference on checkout
		sessionMap.put("buy", buyForm);

		// check customer logged in session if not signin go to signin unlesss go to
		// checkout
		String view = customerDetail.getId() == 0 ? "signin" : "checkout";
		ModelAndView modelAndView = new ModelAndView(view);

		if (customerDetail.getId() == 0) {
			SignIn signIn = new SignIn();

			modelAndView.addObject("signin", signIn);
			sessionMap.put("from", "buy");

		} else {
			// get selected book detail.
			Response<BookDetail> bookResponse = bookStoreService.getBookService().getBook(buyForm.getBookId());

			if (Status.SUCCESS.equalsIgnoreCase(bookResponse.getStatus())) {
				// for show detail on page
				modelAndView.addObject("buy", buyForm);
				modelAndView.addObject("customerDetail", customerDetail);
				modelAndView.addObject("bookDetail", bookResponse.getResult());
			} else {
				// if book not found show error
				modelAndView.addObject("error", bookResponse.getMessage());
			}

		}

		return modelAndView;
	}

	/**
	 * mapping when click on sign in
	 * 
	 * @param model
	 * @param signin
	 * @param sessionMap
	 * @return
	 */
	@PostMapping("/doSignIn")
	public String doSignIn(Model model, @ModelAttribute SignIn signin, @ModelAttribute("maps") Map sessionMap) {
		// cal customer service to do authentication
		Response<CustomerDetail> response = bookStoreService.getCustomerService().doAuthen(signin);

		// if user authen success
		if (Status.SUCCESS.equalsIgnoreCase(response.getStatus())) {
			sessionMap.put("customerDetail", response.getResult());

			String from = (String) sessionMap.get("from");
			sessionMap.remove("from");

			// check if logged in from history page
			if ("history".equalsIgnoreCase(from)) {
				return history(model, sessionMap);

			} else {
				// user logged in from home page to buy book
				BuyDetail buyCommand = (BuyDetail) sessionMap.get("buy");
				Response<BookDetail> bookResponse = bookStoreService.getBookService().getBook(buyCommand.getBookId());

				if (Status.SUCCESS.equalsIgnoreCase(bookResponse.getStatus())) {
					model.addAttribute("buy", buyCommand);
					model.addAttribute("customerDetail", response.getResult());
					model.addAttribute("bookDetail", bookResponse.getResult());
				} else {
					model.addAttribute("error", bookResponse.getMessage());
				}
				return "checkout";
			}

		} else {
			// if sign in fail . incorrect login redirect to sign in.
			model.addAttribute("signin", signin);
			model.addAttribute("error", response.getMessage());

			return home(model);
		}
	}

	/**
	 * mapping when confirm order
	 * 
	 * @param model
	 * @param sessionMap
	 * @return
	 */
	@PostMapping("/confirmOrder")
	public String confirmOrder(Model model, @ModelAttribute("maps") Map sessionMap) {
		BuyDetail buyCommand = (BuyDetail) sessionMap.get("buy");
		// get customer detail from session
		CustomerDetail customerDetail = (CustomerDetail) sessionMap.get("customerDetail");

		// call order service to save
		Response<OrderDetail> orderResponse = bookStoreService.getOrderService().saveOrder(customerDetail, buyCommand);

		if (Status.SUCCESS.equalsIgnoreCase(orderResponse.getStatus())) {
			Response<List<OrderDetail>> historyResponse = bookStoreService.getOrderService()
					.getHistoryOrder(customerDetail);

			if (Status.SUCCESS.equalsIgnoreCase(historyResponse.getStatus())) {
				model.addAttribute("history", historyResponse.getResult());
			} else {
				model.addAttribute("error", historyResponse.getMessage());
			}

			// show history page after success
			return "history";

		} else {

			// if any error found redirect to home page
			model.addAttribute("error", orderResponse.getMessage());
			return home(model);
		}
	}

	@GetMapping("/history")
	public String history(Model model, @ModelAttribute("maps") Map sessionMap) {
		CustomerDetail customerDetail = (CustomerDetail) sessionMap.get("customerDetail");

		String view = customerDetail.getId() == 0 ? "signin" : "history";

		// check from session. if not logged in go to sign in page
		if (customerDetail.getId() == 0) {
			SignIn signIn = new SignIn();
			signIn.setEmail("steve@email.com");

			model.addAttribute("signin", signIn);

			sessionMap.put("from", "history");
		} else {
			// if user logged in get order history from order service.
			Response<List<OrderDetail>> historyResponse = bookStoreService.getOrderService()
					.getHistoryOrder(customerDetail);

			if (Status.SUCCESS.equalsIgnoreCase(historyResponse.getStatus())) {
				model.addAttribute("history", historyResponse.getResult());
			} else {
				model.addAttribute("error", historyResponse.getMessage());
			}

		}

		return view;

	}
}
