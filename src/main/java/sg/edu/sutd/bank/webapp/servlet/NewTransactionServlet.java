/*
 * Copyright 2017 SUTD Licensed under the
	Educational Community License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may
	obtain a copy of the License at

https://opensource.org/licenses/ECL-2.0

	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an "AS IS"
	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
	or implied. See the License for the specific language governing
	permissions and limitations under the License.
 */

package sg.edu.sutd.bank.webapp.servlet;

import sg.edu.sutd.bank.webapp.commons.ServiceException;
import sg.edu.sutd.bank.webapp.model.ClientAccount;
import sg.edu.sutd.bank.webapp.model.ClientTransaction;
import sg.edu.sutd.bank.webapp.model.User;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAO;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAOImpl;
import sg.edu.sutd.bank.webapp.service.ClientTransactionDAO;
import sg.edu.sutd.bank.webapp.service.ClientTransactionDAOImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static sg.edu.sutd.bank.webapp.servlet.ServletPaths.NEW_TRANSACTION;

@WebServlet(NEW_TRANSACTION)
public class NewTransactionServlet extends DefaultServlet {
	private static final long serialVersionUID = 1L;
	private ClientTransactionDAO clientTransactionDAO = new ClientTransactionDAOImpl();
	private ClientAccount clientAccount = new ClientAccount();
	private ClientAccountDAO clientAccountDAO = new ClientAccountDAOImpl();
	BigDecimal bi1,bi2;


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			ClientTransaction clientTransaction = new ClientTransaction();
			User user = new User(getUserId(req));
			clientTransaction.setUser(user);
			clientTransaction.setAmount(new BigDecimal(req.getParameter("amount")));
			clientTransaction.setTransCode(req.getParameter("transcode"));
			clientTransaction.setToAccountNum(req.getParameter("toAccountNum"));
			clientTransactionDAO.create(clientTransaction);
			
									
			bi1 = new BigDecimal("-1");
			bi2 = new BigDecimal("10");
			if ((clientTransaction.getAmount()).compareTo(bi2)==-1);	{		
				//Sender
				clientAccount.setUser(user);
				clientAccount.setId(getUserId(req));
				clientAccount.setAmount((clientTransaction.getAmount()).multiply(bi1));
				clientAccountDAO.update(clientAccount);
				
				//Receiver
				String clientId = req.getParameter("toAccountNum");
				User user1 = new User(Integer.parseInt(clientId));
				clientAccount.setUser(user1);
				clientAccount.setId(getUserId(req));
				clientAccount.setAmount((clientTransaction.getAmount()));
				clientAccountDAO.update(clientAccount);
				redirect(resp, ServletPaths.CLIENT_DASHBOARD_PAGE);
			}			
				
			
		} catch (ServiceException e) {
			sendError(req, e.getMessage());
			forward(req, resp);
		}
	}
}
