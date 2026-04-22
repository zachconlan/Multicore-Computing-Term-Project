# Parallel Housing Allocation Term Project

This project implements versions of the housing allocation problem using LLP. Agents are assigned to houses based on their preferences using a top-trading-cycles-style approach.

**Implementations**

HousingAllocation: strict preferences

HousingAllocationWithTies: handles tied preferences

MultiUnitHousingAllocation: supports multiple units per house

EligibilityHousingAllocation: adds eligibility constraints


**LLP**

forbidden(j): checks if an agent must advance

advance(j): moves the agent to the next option in their preference list

**Testing**

JUnit tests used to verify all implementations.
