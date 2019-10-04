package org.eqasim.switzerland.ovgk;

public enum OVGK {
	A, B, C, D, None;

	static public int rank(OVGK ovgk) {
		switch (ovgk) {
		case A:
			return 5;
		case B:
			return 4;
		case C:
			return 3;
		case D:
			return 2;
		case None:
			return 1;
		}

		throw new IllegalStateException();
	}

	static public OVGK best(OVGK a, OVGK b) {
		int rankA = rank(a);
		int rankB = rank(b);

		if (rankA > rankB) {
			return a;
		} else {
			return b;
		}
	}

	static public OVGK worst(OVGK a, OVGK b) {
		int rankA = rank(a);
		int rankB = rank(b);

		if (rankA < rankB) {
			return a;
		} else {
			return b;
		}
	}
}
