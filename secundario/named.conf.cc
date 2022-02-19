zone "cc.pt" {
	type slave;
	file "/var/cache/bind/db.cc.pt";
	masters {10.2.2.1;}; // primário
};

zone "1.1.10.in-addr.arpa" {
	type slave;
	file "/var/cache/bind/db.1-1-10.rev";
	masters {10.2.2.1;}; // primário
};

zone "2.2.10.in-addr.arpa" {
	type slave;
	file "/var/cache/bind/db.2-2-10.rev";
	masters {10.2.2.1;}; // primário
};

zone "3.3.10.in-addr.arpa" {
	type slave;
	file "/var/cache/bind/db.3-3-10.rev";
	masters {10.2.2.1;}; // primário
};

zone "4.4.10.in-addr.arpa" {
	type slave;
	file "/var/cache/bind/db.4-4-10.rev";
	masters {10.2.2.1;}; // primário
};

