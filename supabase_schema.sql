-- Run this in your Supabase SQL Editor

-- 1. Create the transport_activities table
CREATE TABLE transport_activities (
  id UUID PRIMARY KEY,
  transportMode TEXT NOT NULL,
  startTimestampMs BIGINT NOT NULL,
  endTimestampMs BIGINT NOT NULL,
  distanceMeters REAL NOT NULL,
  avgSpeedMps REAL NOT NULL,
  co2KgEmitted REAL NOT NULL,
  companyId TEXT NOT NULL,
  isAuditVerified BOOLEAN DEFAULT FALSE,
  user_id UUID REFERENCES auth.users(id) -- Links to the JWT Auth user
);

-- Enable Row Level Security (RLS) so companies can only see their own data
ALTER TABLE transport_activities ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can insert their own activities" 
ON transport_activities FOR INSERT 
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can view their own activities" 
ON transport_activities FOR SELECT 
USING (auth.uid() = user_id);


-- 2. Create the sustainability_reports table
CREATE TABLE sustainability_reports (
  id UUID PRIMARY KEY,
  companyId TEXT NOT NULL,
  reportPeriodStart BIGINT NOT NULL,
  reportPeriodEnd BIGINT NOT NULL,
  totalCo2Kg REAL NOT NULL,
  offsetPurchased REAL NOT NULL,
  netCo2Kg REAL NOT NULL,
  executiveSummaryJson TEXT NOT NULL,
  pdfBlobPath TEXT,
  createdAt BIGINT NOT NULL,
  isLegallySubmitted BOOLEAN DEFAULT FALSE,
  user_id UUID REFERENCES auth.users(id) -- Links to the JWT Auth user
);

-- Enable Row Level Security (RLS)
ALTER TABLE sustainability_reports ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can insert their own reports" 
ON sustainability_reports FOR INSERT 
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can view their own reports" 
ON sustainability_reports FOR SELECT 
USING (auth.uid() = user_id);
