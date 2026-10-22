package org.bgjug.jprime.site.web;

import io.quarkus.qute.TemplateData;
import org.bgjug.jprime.core.model.Article;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Partner;
import org.bgjug.jprime.core.model.Session;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.model.Sponsor;
import org.bgjug.jprime.core.model.SponsorPackage;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.model.Tag;
import org.bgjug.jprime.core.model.TicketPrice;
import org.bgjug.jprime.core.model.VenueHall;
import org.bgjug.jprime.core.service.PageResult;

/**
 * Makes Qute generate reflection-free value resolvers for the domain classes used in templates.
 */
@TemplateData(target = Article.class)
@TemplateData(target = Branch.class)
@TemplateData(target = Partner.class)
@TemplateData(target = Session.class)
@TemplateData(target = Speaker.class)
@TemplateData(target = Sponsor.class)
@TemplateData(target = SponsorPackage.class)
@TemplateData(target = org.bgjug.jprime.core.model.SessionLevel.class)
@TemplateData(target = org.bgjug.jprime.core.model.SessionType.class)
@TemplateData(target = Submission.class)
@TemplateData(target = Tag.class)
@TemplateData(target = TicketPrice.class)
@TemplateData(target = VenueHall.class)
@TemplateData(target = PageResult.class)
@TemplateData(target = CfpForm.class)
@TemplateData(target = TicketForm.class)
public final class Views {

    private Views() {
    }
}
