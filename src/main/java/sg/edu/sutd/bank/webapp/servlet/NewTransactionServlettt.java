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

import static sg.edu.sutd.bank.webapp.servlet.ServletPaths.NEW_TRANSACTION;
import static sg.edu.sutd.bank.webapp.service.AbstractDAOImpl.connectDB;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.validator.routines.DoubleValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.RegexValidator;
import sg.edu.sutd.bank.webapp.commons.Constants;

import sg.edu.sutd.bank.webapp.commons.ServiceException;
import sg.edu.sutd.bank.webapp.commons.ValidatorRegex;
import sg.edu.sutd.bank.webapp.model.ClientAccount;
import sg.edu.sutd.bank.webapp.model.ClientTransaction;
import sg.edu.sutd.bank.webapp.model.TransactionStatus;
import sg.edu.sutd.bank.webapp.model.User;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAO;
import sg.edu.sutd.bank.webapp.service.ClientAccountDAOImpl;
import sg.edu.sutd.bank.webapp.service.ClientTransactionDAO;
import sg.edu.sutd.bank.webapp.service.ClientTransactionDAOImpl;
import sg.edu.sutd.bank.webapp.service.TransactionCodesDAO;
import sg.edu.sutd.bank.webapp.service.TransactionCodesDAOImp;

@WebServlet(NEW_TRANSACTION)
public class NewTransactionServlet extends DefaultServlet {

    private static final long serialVersionUID = 1L;
    private final ClientTransactionDAO clientTransactionDAO = new ClientTransactionDAOImpl();
    private final ClientAccountDAO clientAccountDAO = new ClientAccountDAOImpl();
    private final TransactionCodesDAO transactionCodesDAO = new TransactionCodesDAOImp();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /**
         * Sanitization and validation
         */
        int user_id = 0;

        Connection conn = null;

        try {
            user_id = getUserId(req);

            String amount = req.getParameter("amount");
            String transcode = req.getParameter("transcode");
            String toAccountNum = req.getParameter("toAccountNum");

            if (!DoubleValidator.getInstance().isValid(amount)) {
                throw new SQLException("Amount is not valid");
            } else if (!new RegexValidator(ValidatorRegex.TRANSACTION_CODE).isValid(transcode)) {
                throw new SQLException("Transaction code is not valid");
            } else if (!IntegerValidator.getInstance().isValid(toAccountNum)) {
                throw new SQLException("Account destination is not valid");
            }
            
            conn = connectDB();
            conn.setAutoCommit(false);

            ClientTransaction transaction = new ClientTransaction();

            User user = new User(user_id);
            transaction.setUser(user);

            ClientAccount account = clientAccountDAO.loadAccount(user);

            transaction.setAmount(new BigDecimal(amount));
            transaction.setTransCode(transcode);
            transaction.setToAccountNum(Integer.parseInt(toAccountNum));

            if (account.getAmount().compareTo(transaction.getAmount()) < 0) {
                throw new SQLException("No enough money");
            } else if (!transactionCodesDAO.validateTransactionCode(transaction.getTransCode(), transaction.getUser().getId(), conn)) {
                throw new SQLException("Transaction code is not valid");
            } else {
                if (transaction.getAmount().compareTo(Constants.TRANSACTION_LIMIT) < 0) {
                    transaction.setStatus(TransactionStatus.APPROVED);
                    clientAccountDAO.executeTransaction(transaction, conn);
                } else {
                    transaction.setStatus(TransactionStatus.WAITING);
                }
                account.setAmount(account.getAmount().subtract(transaction.getAmount()));
            }
            clientAccountDAO.update(account, conn);
            clientTransactionDAO.create(transaction, conn);

            conn.commit();

            redirect(resp, ServletPaths.CLIENT_DASHBOARD_PAGE);
        } catch (ServiceException | SQLException e) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
            }
            sendError(req, e.getMessage());
            forward(req, resp);
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ex) {
            }
        }
    }
}
