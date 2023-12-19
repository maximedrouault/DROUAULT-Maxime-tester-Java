package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        when(inputReaderUtil.readSelection()).thenReturn(1);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        //TODO: check that a ticket is actually saved in DB and Parking table is updated with availability
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(ticket, "Ticket must not be null");
        assertNotNull(ticket.getInTime(), "The in time must be set in the ticket");
        assertEquals(0,ticket.getPrice(), "The ticket price must be set at 0$");
        assertFalse(ticket.getParkingSpot().isAvailable(), "ParkingSpot must be unavailable");
    }

    @Test
    public void testParkingLotExit() throws InterruptedException {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Create and save a ticket with inTime simulated, one hour before here.
        createTicketWithSimulatedIntime();

        parkingService.processExitingVehicle();

        //TODO: check that the fare generated and out time are populated correctly in the database
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(ticket, "Ticket must not be null");
        assertEquals(1.50, ticket.getPrice(), 0.01, "The ticket price must not be close to 1.50$");
        assertNotNull(ticket.getOutTime(), "The out time must be set in the ticket");
        assertEquals(1, ticketDAO.getNbTicket("ABCDEF"), "The number of tickets must be 2");
    }

    @Test
    public void testParkingLotExitRecurringUser(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Create a ticket with in and outTime to simulate a first parking.
        // inTime three hours before and outTime two hours before here.
        createTicketWithSimulatedInAndOutTime();
        // Create a ticket with inTime to simulate second entry to check the discount price for regular user.
        // inTime one hour before here.
        createTicketWithSimulatedIntime();

        parkingService.processExitingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(ticket, "Ticket must not be null");
        assertEquals(1.43, ticket.getPrice(), 0.01, "The ticket price must not be close to 1.43$"); // Price with regular user discount.
        assertEquals(2, ticketDAO.getNbTicket("ABCDEF"), "The number of tickets must be 2");
    }

    // Method to create and save a ticket with simulated inTime and outTime defined as arguments.
    private void createSimulatedTicket(Date inTime, Date outTime, double price) {
        Ticket simulatedTicket = new Ticket();
        simulatedTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        simulatedTicket.setVehicleRegNumber("ABCDEF");
        simulatedTicket.setPrice(price);
        simulatedTicket.setInTime(inTime);
        simulatedTicket.setOutTime(outTime);

        ticketDAO.saveTicket(simulatedTicket);
    }

    // Create and save a ticket with simulated inTime one hour ago here.
    public void createTicketWithSimulatedIntime() {
        Date inTime = new Date(System.currentTimeMillis() - 60 * 60 * 1000); // inTime one hour ago.

        createSimulatedTicket(inTime, null, 0);
    }

    // Create and save a ticket with simulated inTime three hours ago and outTime two hours ago here.
    public void createTicketWithSimulatedInAndOutTime() {
        Date inTime = new Date(System.currentTimeMillis() - 60 * 60 * 3000); // inTime three hours ago.
        Date outTime = new Date(System.currentTimeMillis() - 60 * 60 * 2000); // outTime two hours ago.

        createSimulatedTicket(inTime, outTime, 1.5);
    }
}
