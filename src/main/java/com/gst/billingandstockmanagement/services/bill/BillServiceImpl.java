package com.gst.billingandstockmanagement.services.bill;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gst.billingandstockmanagement.dto.BillDTO;
import com.gst.billingandstockmanagement.entities.Bill;
import com.gst.billingandstockmanagement.entities.BillItems;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.BillRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import com.gst.billingandstockmanagement.repository.BillItemsRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillServiceImpl implements BillService {

    @Autowired
    private BillRepository billRepository;
    
    @Autowired
    private BillItemsRepository billItemsRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public BillDTO addBill(BillDTO billDTO) {
        // Fetch the User entity from the database using the userId
        User user = userRepository.findById(billDTO.getUserId()).orElse(null);

        // Convert BillDTO to Bill entity
        Bill bill = new Bill();
        bill.setId(billDTO.getId());
        bill.setUser(user);
        bill.setPurchaserName(billDTO.getPurchaserName());
        bill.setDl1(billDTO.getDl1());
        bill.setDl2(billDTO.getDl2());
        bill.setGstin(billDTO.getGstin());
        bill.setInvoiceDate(billDTO.getInvoiceDate());

        // Save the Bill entity using billRepository
        Bill savedBill = billRepository.save(bill);

        // Calculate and update the total amount of the bill
        updateTotalAmount(savedBill);
        
        BillDTO savedBillDTO = new BillDTO();
        savedBillDTO.setId(savedBill.getId());
        savedBillDTO.setUserId(savedBill.getUser().getId());
        savedBillDTO.setPurchaserName(savedBill.getPurchaserName());
        savedBillDTO.setDl1(savedBill.getDl1());
        savedBillDTO.setDl2(savedBill.getDl2());
        savedBillDTO.setGstin(savedBill.getGstin());
        savedBillDTO.setInvoiceDate(savedBill.getInvoiceDate());

        return savedBillDTO;
    }

    // Helper method to update the total amount of a bill
    public void updateTotalAmount(Bill bill) {
        Double totalAmount = billRepository.calculateTotalAmount(bill);
        if (totalAmount == null) {
            totalAmount = 0.0;
        }
        bill.setTotalAmount(totalAmount);
        billRepository.save(bill); // Update the bill with the new total amount
    }
    
    @Override
    public Bill getBillById(Long billId) {
        // Fetch the Bill entity by its ID
        Bill bill = billRepository.findById(billId).orElse(null);

        if (bill != null) {
            // Fetch the associated BillItems
            List<BillItems> billItems = bill.getBillItems();
            // You can now access the associated BillItems through billItems
        }

        return bill;
    }
    
    @Override
    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }
    
    @Override
    public List<BillDTO> getAllBillsByUser(Long userId) {
        // Fetch the User entity from the database using the userId
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            // Handle the case where the user is not found
            throw new RuntimeException("User not found with ID: " + userId);
        }

        // Fetch all bills associated with the user
        List<Bill> userBills = billRepository.findByUser(user);

        // Convert the list of Bill entities to a list of BillDTOs
        List<BillDTO> userBillDTOs = userBills.stream()
                .map(this::convertToBillDTO)
                .collect(Collectors.toList());

        return userBillDTOs;
    }

    // Helper method to convert a Bill entity to a BillDTO
    private BillDTO convertToBillDTO(Bill bill) {
        BillDTO billDTO = new BillDTO();
        billDTO.setId(bill.getId());
        billDTO.setUserId(bill.getUser().getId());
        billDTO.setPurchaserName(bill.getPurchaserName());
        billDTO.setDl1(bill.getDl1());
        billDTO.setDl2(bill.getDl2());
        billDTO.setGstin(bill.getGstin());
        billDTO.setInvoiceDate(bill.getInvoiceDate());
        // You can set other properties as needed

        return billDTO;
    }

    @Override
    public void deleteBill(Long billId) {
        // Fetch the Bill entity by its ID
        Bill bill = billRepository.findById(billId).orElse(null);

        if (bill != null) {
            // Fetch the associated BillItems
            List<BillItems> billItems = bill.getBillItems();

            // Delete each BillItem
            for (BillItems item : billItems) {
                billItemsRepository.delete(item);
            }

            // Delete the Bill
            billRepository.delete(bill);
        } else {
            throw new RuntimeException("Bill not found with ID: " + billId);
        }
    }

}
