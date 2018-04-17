The following is the structure of the CDB, used for testing

Supervisor-ID1 runs DasuID1
	DasuID1 owns no ASCE
Supervisor-ID2 runs nothing
Supervisor-ID3 runs DasuID2, DasuID3, DasuID4
	DasuID2 owns ASCE-ID1
		ASCE-ID1 inputs: iasioID-1, iasioID-2
	DasuID3 owns ASCE-ID2, ASCE-ID3, ASCE-ID4
		ASCE-ID3 inputs: iasioID-1, iasioID-2
		ASCE-ID4 inputs: iasioID-2, iasioID-3, iasioID-4
Supervisor-ID4 runs DasuID5 (instance 3 of template1-ID) and DasuID6 (instance 3 of template3-ID)
	DasuID5 runs
	DasuID6 owns ASCE-ID5 and ASCE-ID6
		ASCE-ID5 inputs iasioID-5, iasioID-6
		ASCE-ID6 inputs iasioID-5, iasioID-7