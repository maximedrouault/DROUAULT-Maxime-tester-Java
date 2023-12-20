package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    private Ticket ticket;
    private ParkingSpot parkingSpot;

    @BeforeEach
    private void setUpPerTest() {
        try {
            parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        parkingService.processExitingVehicle();

        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));

        assertNotNull(ticket.getOutTime(), "The ticket out time must not be Null");
        assertTrue(parkingSpot.isAvailable(), "The parkingSpot must be marked as available after exiting the vehicle");
    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
        verify(ticketDAO, times(1)).getNbTicket("ABCDEF");

        assertEquals(0, ticket.getPrice(), "The ticket price must be 0");
        assertFalse(parkingSpot.isAvailable(), "The parkingSpot must be marked as unavailable after parking");
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).getTicket("ABCDEF");
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));

        assertFalse(ticketDAO.updateTicket(any(Ticket.class)));
    }

    @Test
    public void testGetNextParkingNumberIfAvailable(){
        when(inputReaderUtil.readSelection()).thenReturn(2);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE)).thenReturn(1);

        ParkingSpot returnedParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.BIKE);

        assertNotNull(returnedParkingSpot, "The parkingSpot must not be null");
        assertEquals(1, returnedParkingSpot.getId(), "The parkingSpotId must be 1");
        assertTrue(returnedParkingSpot.isAvailable(), "The parkingSpot must be marked as available");
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound(){
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);

        ParkingSpot returnedParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);

        assertNull(returnedParkingSpot, "No parkingSpot must be available");
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument(){
        when(inputReaderUtil.readSelection()).thenReturn(3);

        ParkingSpot returnedParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        verify(inputReaderUtil, times(1)).readSelection();

        assertNull(returnedParkingSpot, "Should return null due to wrong vehicle type input");
    }

    @Test
    public void testProcessIncomingVehicleIfRegularUser() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processIncomingVehicle();

        verify(ticketDAO, times(1)).getNbTicket("ABCDEF");
        assertTrue(parkingService.isRegularUser("ABCDEF"), "User must be regular");
    }

    @Test
    public void testProcessExitingVehicleIfRegularUser() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        parkingService.processExitingVehicle();


        verify(ticketDAO, times(1)).getNbTicket("ABCDEF");
        assertTrue(parkingService.isRegularUser("ABCDEF"), "User must be regular");
    }

    @Test
    public void processIncomingVehicleException() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenThrow(new RuntimeException("Database failure"));

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, never()).saveTicket(any(Ticket.class));
    }

    @Test
    public void processExitingVehicleException() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenThrow(new RuntimeException("Database failure"));

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).getTicket(anyString());
        verify(ticketDAO, never()).getNbTicket(anyString());
    }


    ///// TEST ////
    @Test
    @Disabled
    public void getVehicleType_ShouldThrowException_WhenSelectionIsInvalid() {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(3);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> parkingService.getVehicleType());
    }

    ///////////////
}