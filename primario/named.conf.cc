zone "cc.pt" {
	type master;
	file "/home/core/primario/db.cc.pt";
	allow-transfer {10.3.3.2;}; // secundário
};

zone "1.1.10.in-addr.arpa" {
	type master;
	file "/home/core/primario/db.1-1-10.rev";
	allow-transfer {10.3.3.2;}; // secundário
};

zone "2.2.10.in-addr.arpa" {
	type master;
	file "/home/core/primario/db.2-2-10.rev";
	allow-transfer {10.3.3.2;}; // secundário
};

zone "3.3.10.in-addr.arpa" {
	type master;
	file "/home/core/primario/db.3-3-10.rev";
	allow-transfer {10.3.3.2;}; // secundário
};

zone "4.4.10.in-addr.arpa" {
	type master;
	file "/home/core/primario/db.4-4-10.rev";
	allow-transfer {10.3.3.2;}; // secundário
};

