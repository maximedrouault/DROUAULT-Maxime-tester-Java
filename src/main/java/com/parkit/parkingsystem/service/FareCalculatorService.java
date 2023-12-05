package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        // The timestamps are subtracted and converted from Milliseconds to Hours to deduct the duration of parking in part of hour.
        // (1000 Milliseconds * 60 seconds * 60 minutes)
        final int numberOfMillisecondsInAnHour = 1000 * 60 * 60;
        double duration = (double) (outHour - inHour) / numberOfMillisecondsInAnHour;

        // If the duration of parking is less than 30 minutes (0.5h), the price is 0$ (free).
        if (duration <= 0.5){
            ticket.setPrice(0);
        } else {

            // Else, the price is calculating with the duration of parking.
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    // Added 5% discount for regular car users.
                    if (discount) {
                        ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR * 0.95);
                    // Full price for non-regular users.
                    } else {
                        ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                    }
                break;
                }
                case BIKE: {
                    // Added 5% discount for regular bike users.
                    if (discount) {
                        ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR * 0.95);
                    // Full price for non-regular users.
                    } else {
                    ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                    }
                break;
                }
                default:
                    throw new IllegalArgumentException("Unknown Parking Type");
            }
        }
    }

    public void calculateFare(Ticket ticket){
        calculateFare(ticket, false);
    }

}