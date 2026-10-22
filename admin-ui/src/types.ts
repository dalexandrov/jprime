// JSON shapes of the jprime-admin API (see org.bgjug.jprime.admin.dto.Dtos)

export type TicketType = 'REGULAR' | 'EARLY_BIRD' | 'STUDENT';
export type SubmissionStatus = 'SUBMITTED' | 'ABORTED' | 'REJECTED' | 'ACCEPTED' | 'CONFIRMED' | 'CANCELED';
export type VisitorStatus = 'REQUESTING' | 'PAYED' | 'SPONSORED';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Option {
  value: string;
  label: string;
}

export interface Lookups {
  currentYear: number;
  years: number[];
  enums: Record<string, Option[]>;
}

export interface Branch {
  year: number;
  label: string;
  startDate?: string;
  durationHours: number;
  durationText: string;
  cfpOpenDate?: string;
  cfpCloseDate?: string;
  soldOut: boolean;
  agendaPublished: boolean;
  currentBranch: boolean;
  soldOutPackages: string[];
  ticketPrices: Partial<Record<TicketType, number>>;
}

export interface Tag {
  id?: number;
  name: string;
}

export interface Article {
  id?: number;
  title: string;
  description?: string;
  published: boolean;
  text?: string;
  tags: Tag[];
  createdDate?: string;
  createdBy?: string;
}

export interface Sponsor {
  id?: number;
  companyName?: string;
  companyWebsite?: string;
  email?: string;
  description?: string;
  sponsorPackage?: string;
  active: boolean;
}

export interface Partner {
  id?: number;
  companyName?: string;
  companyWebsite?: string;
  description?: string;
  partnerPackage?: string;
  active: boolean;
}

export interface SubmissionSummary {
  id: number;
  title: string;
  branchYear?: number;
  status?: SubmissionStatus;
  featured: boolean;
}

export interface Speaker {
  id?: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  headline?: string;
  twitter?: string;
  bsky?: string;
  bio?: string;
  speakerType?: string;
  numberOfSubmissions?: number;
  featured?: boolean;
  status?: SubmissionStatus;
  lastBranchYear?: number;
  submissions?: SubmissionSummary[];
}

export interface PersonRef {
  id: number;
  firstName?: string;
  lastName?: string;
  email?: string;
}

export interface Submission {
  id: number;
  title: string;
  description: string;
  level?: string;
  type?: string;
  status: SubmissionStatus;
  featured: boolean;
  branchYear?: number;
  speaker?: PersonRef;
  coSpeaker?: PersonRef;
  createdDate?: string;
}

export interface SpeakerFields {
  firstName?: string;
  lastName?: string;
  email?: string;
  headline?: string;
  twitter?: string;
  bsky?: string;
  bio?: string;
}

export interface SubmissionInput {
  title: string;
  description: string;
  level?: string;
  type?: string;
  branchYear?: number;
  featured?: boolean;
  speaker: SpeakerFields;
  coSpeaker?: SpeakerFields | null;
}

export interface Hall {
  id?: number;
  name: string;
  description?: string;
}

export interface Session {
  id?: number;
  title?: string;
  startTime?: string;
  endTime?: string;
  submissionId?: number;
  submissionTitle?: string;
  speakers?: string;
  hallId?: number;
  hallName?: string;
}

export interface Visitor {
  id?: number;
  name: string;
  email?: string;
  company?: string;
  status?: VisitorStatus;
  ticket?: string;
  present?: boolean;
  registered?: boolean;
  registrantId?: number;
  registrantName?: string;
  registrantInvoiceNumber?: number;
  createdDate?: string;
}

export interface VisitorList {
  visitors: Visitor[];
  stats: { requesting: number; payed: number; sponsored: number; total: number };
}

export interface Registrant {
  id?: number;
  name: string;
  email: string;
  company: boolean;
  student: boolean;
  address?: string;
  vatNumber?: string;
  eik?: string;
  mol?: string;
  paymentType?: string;
  epayInvoiceNumber?: number;
  realInvoiceNumber?: number;
  proformaInvoiceNumber?: number;
  branchYear?: number;
  visitorCount?: number;
  visitors?: Visitor[];
  createdDate?: string;
}

export interface Invoice {
  registrantId: number;
  invoiceNumber: string;
  invoiceDate: string;
  client?: string;
  clientAddress?: string;
  clientEIK?: string;
  clientVAT?: string;
  mol?: string;
  paymentType?: string;
  passQty: number;
  singlePriceWithVAT: number;
  description?: string;
  totalPrice: number;
  totalPriceVAT: number;
  totalPriceWithVAT: number;
}

export interface User {
  id: number;
  firstName?: string;
  lastName?: string;
  email: string;
  phone?: string;
  type: string;
}

export interface Job {
  jobId: string;
  description: string;
  status?: string;
  log?: string;
  completed?: string;
}

export interface SendResult {
  sent: number;
  failed: number;
  jobId?: string;
}

export interface Message {
  message: string;
}

export interface RaffleEntry {
  name: string;
  company?: string;
}
