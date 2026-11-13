-- Demo listings so the directory is not empty during development. Safe to run more than once.
-- Names and places are made up. Phone numbers are deliberately invalid (0000000001 ...) so a test call
-- can never ring a real person. Coordinates are around Bhopal. Remove before launch:
--   delete from public.partners where is_demo;
delete from public.partners where is_demo;

insert into public.partners
  (is_demo, user_id, full_name, village, district, category, sub_categories, service_areas, indicative_rate,
   insurance_until, fitness_until, phone, phone_public, latitude, longitude)
values
  (true, null, 'Ramesh Yadav',  'Arera Colony', 'Bhopal', 'plumber',      '{"Leak repair","Bathroom fitting"}', '{"Arera Colony","MP Nagar"}', null,        null, null, '0000000001', true, 23.22, 77.43),
  (true, null, 'Suresh Patel',  'Nandgaon',     'Bhopal', 'jcb',          '{"Excavation","Loading"}',           '{"Nandgaon","Kolar"}',        '₹1,100 / hour', '2035-12-31', '2035-12-31', '0000000002', true, 23.31, 77.36),
  (true, null, 'Imran Khan',    'Kolar',        'Bhopal', 'borewell',     '{"Borewell drilling"}',              '{"Kolar","Misrod"}',          null,        '2035-12-31', '2035-12-31', '0000000003', true, 23.18, 77.42),
  (true, null, 'Dinesh Kumar',  'Salempur',     'Sehore', 'construction', '{"Mason (mistri)","Tiles"}',         '{"Sehore"}',                  '₹900 / day', null, null, '0000000004', true, 23.20, 77.09),
  (true, null, 'Anil Verma',    'Bairagarh',    'Bhopal', 'electrician',  '{"House wiring"}',                   '{"Bairagarh","Kolar"}',       null,        null, null, '0000000005', true, 23.28, 77.34),
  (true, null, 'Kishan Lal',    'Chandpur',     'Raisen', 'tractor',      '{"Cultivator","Thresher"}',          '{"Raisen"}',                  '₹800 / hour', '2035-12-31', null, '0000000006', true, 23.33, 77.79),
  (true, null, 'Mohan Sahu',    'Habibganj',    'Bhopal', 'plumber',      '{"Pipe fitting","Water tank"}',      '{"Habibganj","Arera Hills"}', '₹400 / visit', null, null, '0000000007', true, 23.23, 77.44),
  (true, null, 'Geeta Bai',     'Misrod',       'Bhopal', 'construction', '{"Painter"}',                        '{"Misrod","Bawadiya Kalan"}', '₹700 / day', null, null, '0000000008', true, 23.18, 77.47),
  (true, null, 'Salim Ansari',  'Shahpura',     'Bhopal', 'electrician',  '{"Fan / light repair","Inverter / UPS"}', '{"Shahpura","Kolar"}', null,     null, null, '0000000009', true, 23.19, 77.43),
  (true, null, 'Rakesh Jain',   'Govindpura',   'Bhopal', 'transport',    '{}',                                 '{"Govindpura","Mandideep"}',  null,        '2035-12-31', '2035-12-31', '0000000010', true, 23.24, 77.47),
  (true, null, 'Vijay Singh',   'Ratibad',      'Bhopal', 'jcb',          '{"Leveling","Demolition"}',          '{"Ratibad"}',                 null,        '2035-12-31', '2035-12-31', '0000000011', true, 23.15, 77.36),
  (true, null, 'Pappu Lodhi',   'Kolar Road',   'Bhopal', 'construction', '{"Labour","Mason (mistri)"}',        '{"Kolar Road"}',              '₹650 / day', null, null, '0000000012', true, 23.19, 77.42),
  (true, null, 'Hari Prasad',   'Indore Naka',  'Indore', 'plumber',      '{"Leak repair"}',                    '{"Indore"}',                  null,        null, null, '0000000013', true, 22.72, 75.86),
  (true, null, 'Mukesh Rathore','Vijay Nagar',  'Indore', 'borewell',     '{"Borewell drilling","Flushing"}',   '{"Indore"}',                  null,        '2035-12-31', '2035-12-31', '0000000014', true, 22.75, 75.89),
  (true, null, 'Ravi Shankar',  'Dwarka Sector 12', 'Delhi', 'electrician', '{"House wiring"}',               '{"Dwarka"}',                  null,        null, null, '0000000015', true, 28.59, 77.04);
