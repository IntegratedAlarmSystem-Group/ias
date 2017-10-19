The following is the structure of the CDB, used for testing

Supervisor-ID1 ons DasuID1
	DasuID1 owns no ASCE
Supervisor-ID2 owns nothing
Supervisor-ID3 owns DasuID2, DasuID3, DasuID4
	DasuID2 owns ASCE-ID1
		ASCE-ID1 inputs: iasioID-1, iasioID-2
	DasuID3 owns ASCE-ID2, ASCE-ID3, ASCE-ID4
		ASCE-ID3 inputs: iasioID-1, iasioID-2
		ASCE-ID4 inputs: iasioID-2, iasioID-3, iasioID-4