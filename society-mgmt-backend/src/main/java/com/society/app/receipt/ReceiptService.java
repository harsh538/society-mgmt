package com.society.app.receipt;

import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.receipt.dto.ReceiptDto;
import com.society.app.storage.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Page<ReceiptDto> listReceipts(String phone, boolean isAdmin, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("issuedAt").descending());
        if (isAdmin) {
            return receiptRepository.findAll(newestFirst).map(ReceiptDto::from);
        }
        Member m = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));
        return receiptRepository.findByIssuedToId(m.getId(), newestFirst).map(ReceiptDto::from);
    }

    @Transactional(readOnly = true)
    public ReceiptDto getReceipt(Long id, String phone, boolean isAdmin) {
        Receipt r = loadOrThrow(id);
        if (!isAdmin) ensureMemberOwnsReceipt(r, phone);
        return ReceiptDto.from(r);
    }

    @Transactional(readOnly = true)
    public String getReceiptPdf(Long id, String phone, boolean isAdmin) {
        Receipt r = loadOrThrow(id);
        if (!isAdmin) ensureMemberOwnsReceipt(r, phone);
        if (r.getPdfFilePath() == null || r.getPdfFilePath().isBlank()) {
            throw new EntityNotFoundException("Receipt PDF not available for " + id);
        }
        return fileStorageService.getPublicUrl(r.getPdfFilePath());
    }

    private Receipt loadOrThrow(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + id));
    }

    private void ensureMemberOwnsReceipt(Receipt r, String phone) {
        if (r.getIssuedTo() == null || !phone.equals(r.getIssuedTo().getPhone())) {
            throw new AccessDeniedException("Member is not authorized to view this receipt");
        }
    }
}
